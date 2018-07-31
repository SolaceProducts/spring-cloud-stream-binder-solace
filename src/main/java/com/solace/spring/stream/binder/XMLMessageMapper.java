package com.solace.spring.stream.binder;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.XMLMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

class XMLMessageMapper {
	private static final Log logger = LogFactory.getLog(XMLMessageMapper.class);
	private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;
	private static final String MIME_JAVA_SERIALIZED_OBJECT = "application/x-java-serialized-object";

	public XMLMessage map(Message<?> message) {
		Object payload = message.getPayload();
		String mimeType;
		byte[] attachment;
		Charset charset = null;

		if (payload instanceof byte[]) {
			mimeType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
			attachment = (byte[]) payload;
		} else if (payload instanceof String) {
			mimeType = MimeTypeUtils.TEXT_PLAIN_VALUE;
			charset = DEFAULT_ENCODING;
			attachment = ((String) payload).getBytes(charset);
		} else if (payload instanceof Serializable) {
			mimeType = MIME_JAVA_SERIALIZED_OBJECT;
			attachment = SerializationUtils.serialize(payload);
		} else {
			throw new IllegalArgumentException(String.format(
					"Invalid payload received. Expected byte[], String, or Serializable. Received: %s",
					payload.getClass().getName()));
		}

		BytesXMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.writeAttachment(attachment);
		xmlMessage.setHTTPContentType(mimeType);
		if (charset != null) xmlMessage.setHTTPContentEncoding(charset.name());
		return xmlMessage;
	}

	public Message<?> map(XMLMessage xmlMessage) throws MessagingException {
		byte[] attachment = xmlMessage.getAttachmentByteBuffer().array();
		String contentType = xmlMessage.getHTTPContentType();
		String encodingName = xmlMessage.getHTTPContentEncoding();

		Object payload = null;
		if (contentType != null) {
			if (contentType.startsWith(MimeTypeUtils.TEXT_PLAIN.getType())) {
				Charset encoding = StringUtils.hasText(encodingName) ? Charset.forName(encodingName) : DEFAULT_ENCODING;
				payload = new String(attachment, encoding);
			} else if (contentType.equalsIgnoreCase(MIME_JAVA_SERIALIZED_OBJECT)) {
				payload = SerializationUtils.deserialize(attachment);
			}
		}

		return new DefaultMessageBuilderFactory().withPayload(payload != null ? payload : attachment)
				.setHeaderIfAbsent("deliveryAttempt", new AtomicInteger(0))
				.build();
	}
}
