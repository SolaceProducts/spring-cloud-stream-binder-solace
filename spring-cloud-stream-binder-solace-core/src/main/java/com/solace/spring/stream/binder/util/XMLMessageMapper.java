package com.solace.spring.stream.binder.util;

import com.solace.spring.stream.binder.properties.SolaceConsumerProperties;
import com.solace.spring.stream.binder.properties.SolaceProducerProperties;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.XMLMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class XMLMessageMapper {
	private static final Log logger = LogFactory.getLog(XMLMessageMapper.class);
	private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;
	private static final String MIME_JAVA_SERIALIZED_OBJECT = "application/x-java-serialized-object";
	private static final JCSMPAcknowledgementCallbackFactory ackCallbackFactory = new JCSMPAcknowledgementCallbackFactory();

	public XMLMessage map(Message<?> message, SolaceProducerProperties producerProperties) {
		XMLMessage xmlMessage = map(message);
		xmlMessage.setDMQEligible(producerProperties.isMsgDmqEligible());
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

	private XMLMessage map(Message<?> message) {
		byte[] messageWrapperBytes = SerializationUtils.serialize(createMessageWrapper(message));
		BytesXMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.writeAttachment(messageWrapperBytes);
		xmlMessage.setHTTPContentType(MIME_JAVA_SERIALIZED_OBJECT);
		xmlMessage.setDeliveryMode(DeliveryMode.PERSISTENT);
		return xmlMessage;
	}

	public Message<?> map(XMLMessage xmlMessage) throws SolaceMessageConversionException {
		return map(xmlMessage, false);
	}

	public Message<?> map(XMLMessage xmlMessage, boolean setRawMessageHeader) throws SolaceMessageConversionException {
		MessageWrapper messageWrapper = extractMessageWrapper(xmlMessage);

		Object payload = null;
		byte[] payloadBytes = messageWrapper.getPayload();
		String mimeType = messageWrapper.getPayloadMimeType();

		if (mimeType.startsWith(MimeTypeUtils.TEXT_PLAIN.getType())) {
			String encodingName = messageWrapper.getCharset();
			Charset encoding = StringUtils.hasText(encodingName) ? Charset.forName(encodingName) : DEFAULT_ENCODING;
			payload = new String(payloadBytes, encoding);

		} else if (mimeType.equalsIgnoreCase(MIME_JAVA_SERIALIZED_OBJECT)) {
			payload = SerializationUtils.deserialize(payloadBytes);
		}

		MessageBuilder<?> builder =  new DefaultMessageBuilderFactory()
				.withPayload(payload != null ? payload : payloadBytes)
				.copyHeaders(messageWrapper.getHeaders())
				.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, ackCallbackFactory.createCallback(xmlMessage))
				.setHeaderIfAbsent(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, new AtomicInteger(0));

		if (setRawMessageHeader) builder.setHeader(SolaceMessageHeaderErrorMessageStrategy.SOLACE_RAW_MESSAGE, xmlMessage);

		return builder.build();
	}

	private MessageWrapper createMessageWrapper(Message<?> message) {
		Object payload = message.getPayload();
		String mimeType;
		byte[] payloadBytes;
		Charset charset = null;

		if (payload instanceof byte[]) {
			mimeType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
			payloadBytes = (byte[]) payload;
		} else if (payload instanceof String) {
			mimeType = MimeTypeUtils.TEXT_PLAIN_VALUE;
			charset = DEFAULT_ENCODING;
			payloadBytes = ((String) payload).getBytes(charset);
		} else if (payload instanceof Serializable) {
			mimeType = MIME_JAVA_SERIALIZED_OBJECT;
			payloadBytes = SerializationUtils.serialize(payload);
		} else {
			throw new SolaceMessageConversionException(String.format(
					"Invalid payload received. Expected byte[], String, or Serializable. Received: %s",
					payload.getClass().getName()));
		}

		MessageWrapper messageWrapper = new MessageWrapper(message.getHeaders(), payloadBytes, mimeType);
		if (charset != null) messageWrapper.setCharset(charset.name());
		return messageWrapper;
	}

	private MessageWrapper extractMessageWrapper(XMLMessage xmlMessage) throws SolaceMessageConversionException {
		String messageId = xmlMessage.getMessageId();

		String contentType = xmlMessage.getHTTPContentType();
		if (!contentType.equalsIgnoreCase(MIME_JAVA_SERIALIZED_OBJECT)) {
			throw new SolaceMessageConversionException(String.format(
					"Received Solace message %s with an invalid contentType header. Expected %s. Received %s",
					messageId, MIME_JAVA_SERIALIZED_OBJECT, contentType));
		}

		byte[] attachment = xmlMessage.getAttachmentByteBuffer().array();
		Object serializedMessage = SerializationUtils.deserialize(attachment);

		if (serializedMessage == null) {
			throw new SolaceMessageConversionException(String.format("Received Solace message %s with an empty attachment.",
					messageId));
		}
		else if (!(serializedMessage instanceof MessageWrapper)) {
			throw new SolaceMessageConversionException(String.format(
					"Received Solace Message %s with an invalid attachment. Expected %s. Received %s",
					messageId, MessageWrapper.class.getName(), serializedMessage.getClass().getName()));
		}

		return (MessageWrapper) serializedMessage;
	}


	private static class MessageWrapper implements Serializable {
		private MessageHeaders headers;
		private byte[] payload;
		private String payloadMimeType;
		private String charset;

		MessageWrapper(MessageHeaders headers, byte[] payload, String payloadMimeType) {
			this.headers = headers;
			this.payload = payload;
			this.payloadMimeType = payloadMimeType;
		}

		MessageHeaders getHeaders() {
			return headers;
		}

		byte[] getPayload() {
			return payload;
		}

		String getPayloadMimeType() {
			return payloadMimeType;
		}

		void setCharset(String charset) {
			this.charset = charset;
		}

		String getCharset() {
			return charset;
		}
	}
}
