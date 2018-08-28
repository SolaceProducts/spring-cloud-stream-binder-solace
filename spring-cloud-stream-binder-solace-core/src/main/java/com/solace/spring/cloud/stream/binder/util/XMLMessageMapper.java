package com.solace.spring.cloud.stream.binder.util;

import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;
import com.solace.spring.cloud.stream.binder.properties.SolaceProducerProperties;
import com.solacesystems.common.util.ByteArray;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.SerializationUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class XMLMessageMapper {
	private static final Log logger = LogFactory.getLog(XMLMessageMapper.class);
	private static final JCSMPAcknowledgementCallbackFactory ackCallbackFactory = new JCSMPAcknowledgementCallbackFactory();
	static final Set<String> CUSTOM_HEADERS;
	static final String JAVA_SERIALIZED_OBJECT_HEADER = "isJavaSerializedObject";
	private static final String HEADER_JAVA_SERIALIZED_OBJECT_HEADER = "_" + JAVA_SERIALIZED_OBJECT_HEADER + "-";

	static {
		CUSTOM_HEADERS = new HashSet<>();
		CUSTOM_HEADERS.add(JAVA_SERIALIZED_OBJECT_HEADER);
	}

	public XMLMessage map(Message<?> message, SolaceProducerProperties producerProperties) {
		XMLMessage xmlMessage = map(message);
		xmlMessage.setDMQEligible(producerProperties.isMsgInternalDmqEligible());
		if (producerProperties.getMsgTtl() != null) {
			xmlMessage.setTimeToLive(producerProperties.getMsgTtl());
		}
		return xmlMessage;
	}

	public XMLMessage map(Message<?> message, SolaceConsumerProperties consumerProperties) {
		XMLMessage xmlMessage = map(message);
		if (consumerProperties.getRepublishedMsgTtl() != null) {
			xmlMessage.setTimeToLive(consumerProperties.getRepublishedMsgTtl());
		}
		return xmlMessage;
	}

	XMLMessage map(Message<?> message) {
		XMLMessage xmlMessage;
		Object payload = message.getPayload();

		SDTMap metadata = map(message.getHeaders());

		if (payload instanceof byte[]) {
			BytesMessage bytesMessage = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
			bytesMessage.setData((byte[]) payload);
			xmlMessage = bytesMessage;
		} else if (payload instanceof String) {
			TextMessage textMessage = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
			textMessage.setText((String) payload);
			xmlMessage = textMessage;
		} else if (payload instanceof Serializable) {
			BytesMessage bytesMessage = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
			bytesMessage.setData(rethrowableCall(SerializationUtils::serialize, payload));
			rethrowableCall(metadata::putBoolean, JAVA_SERIALIZED_OBJECT_HEADER, true);
			xmlMessage = bytesMessage;
		} else {
			throw new SolaceMessageConversionException(String.format(
					"Invalid payload received. Expected byte[], String, or Serializable. Received: %s",
					payload.getClass().getName()));
		}

		xmlMessage.setProperties(metadata);
		xmlMessage.setDeliveryMode(DeliveryMode.PERSISTENT);
		return xmlMessage;
	}

	public Message<?> map(XMLMessage xmlMessage) throws SolaceMessageConversionException {
		return map(xmlMessage, false);
	}

	public Message<?> map(XMLMessage xmlMessage, boolean setRawMessageHeader) throws SolaceMessageConversionException {
		SDTMap metadata = xmlMessage.getProperties();

		Object payload;
		if (xmlMessage instanceof TextMessage) {
			payload = ((TextMessage) xmlMessage).getText();
		} else if (xmlMessage instanceof BytesMessage) {
			payload = ((BytesMessage) xmlMessage).getData();
			if (metadata != null &&
					metadata.containsKey(JAVA_SERIALIZED_OBJECT_HEADER) &&
					rethrowableCall(metadata::getBoolean, JAVA_SERIALIZED_OBJECT_HEADER)) {
				payload = rethrowableCall(SerializationUtils::deserialize, (byte[]) payload);
			}
		} else {
			String msg = String.format("Invalid message format received. Expected %s or %s. Received: %s",
					TextMessage.class, BytesMessage.class, xmlMessage.getClass());
			SolaceMessageConversionException exception = new SolaceMessageConversionException(msg);
			logger.warn(msg, exception);
			throw exception;
		}

		if (payload == null) {
			String msg = String.format("XMLMessage %s has no payload", xmlMessage.getMessageId());
			SolaceMessageConversionException exception = new SolaceMessageConversionException(msg);
			logger.warn(msg, exception);
			throw exception;
		}

		MessageBuilder<?> builder =  new DefaultMessageBuilderFactory()
				.withPayload(payload)
				.copyHeaders(map(metadata))
				.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, ackCallbackFactory.createCallback(xmlMessage))
				.setHeaderIfAbsent(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, new AtomicInteger(0));

		if (setRawMessageHeader) builder.setHeader(SolaceMessageHeaderErrorMessageStrategy.SOLACE_RAW_MESSAGE, xmlMessage);

		return builder.build();
	}

	private SDTMap map(MessageHeaders headers) {
		SDTMap metadata = JCSMPFactory.onlyInstance().createMap();
		for (Map.Entry<String,Object> header : headers.entrySet()) {
			Object value = header.getValue();

			if (value instanceof UUID) {
				value = SerializationUtils.serialize(value);
				rethrowableCall(metadata::putBoolean, getIsHeaderSerializedMetadataKey(header.getKey()), true);
			}

			rethrowableCall(metadata::putObject, header.getKey(), value);
		}
		return metadata;
	}

	private MessageHeaders map(SDTMap metadata) {
		if (metadata == null) {
			return new MessageHeaders(Collections.emptyMap());
		}

		Map<String,Object> headers = new HashMap<>();
		metadata.keySet().stream()
				.filter(h -> !CUSTOM_HEADERS.contains(h))
				.filter(h -> !h.startsWith(HEADER_JAVA_SERIALIZED_OBJECT_HEADER))
				.forEach(h -> {
					Object value = rethrowableCall(metadata::get, h);

					String isSerializedMetadataKey = getIsHeaderSerializedMetadataKey(h);
					if (metadata.containsKey(isSerializedMetadataKey) &&
							rethrowableCall(metadata::getBoolean, isSerializedMetadataKey)) {
						value = SerializationUtils.serialize(rethrowableCall(metadata::getBytes, h));
					}

					if (value instanceof ByteArray) {
						value = ((ByteArray) value).getBuffer();
					}

					headers.put(h, value);
				});

		return new MessageHeaders(headers);
	}

	String getIsHeaderSerializedMetadataKey(String headerName) {
		return String.format("%s%s", HEADER_JAVA_SERIALIZED_OBJECT_HEADER, headerName);
	}

	private <T,R> R rethrowableCall(ThrowingFunction<T,R> consumer, T var) {
		return consumer.apply(var);
	}

	private <T,U> void rethrowableCall(ThrowingBiConsumer<T,U> consumer, T var0, U var1) {
		consumer.accept(var0, var1);
	}

	@FunctionalInterface
	private interface ThrowingFunction<T,R> extends Function<T,R> {

		@Override
		default R apply(T t) {
			try {
				return applyThrows(t);
			} catch (Exception e) {
				SolaceMessageConversionException wrappedException = new SolaceMessageConversionException(e);
				logger.warn(wrappedException);
				throw wrappedException;
			}
		}

		R applyThrows(T t) throws Exception;
	}

	@FunctionalInterface
	private interface ThrowingBiConsumer<T,U> extends BiConsumer<T,U> {

		@Override
		default void accept(T t, U u) {
			try {
				applyThrows(t, u);
			} catch (Exception e) {
				SolaceMessageConversionException wrappedException = new SolaceMessageConversionException(e);
				logger.warn(wrappedException);
				throw wrappedException;
			}
		}

		void applyThrows(T t, U u) throws Exception;
	}
}
