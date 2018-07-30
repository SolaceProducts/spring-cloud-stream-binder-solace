package com.solace.spring.stream.binder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.XMLMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.MessagingException;

import java.io.IOException;

class XMLMessageMapper {
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final Log logger = LogFactory.getLog(XMLMessageMapper.class);

	public <T> XMLMessage map(Object payload, Class<T> msgType) throws JsonProcessingException {
		BytesXMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		String serializedPayload;

		ObjectWriter writer = objectMapper.writerFor(msgType);
		serializedPayload = writer.writeValueAsString(payload);

		xmlMessage.writeAttachment(serializedPayload.getBytes());
		xmlMessage.setApplicationMessageType(msgType.getName());
		return xmlMessage;
	}

	public <T> T map(XMLMessage xmlMessage) throws MessagingException {
		String payloadStr = new String(xmlMessage.getAttachmentByteBuffer().array());
		String msgType = xmlMessage.getApplicationMessageType();

		if (msgType == null || msgType.trim().equals("")) {
			throw new IllegalArgumentException("type not specified");
		}

		T payload;
		try {
			Class<?> clazz = Class.forName(msgType);
			ObjectReader reader = objectMapper.readerFor(clazz);
			payload = reader.readValue(payloadStr);
		} catch (ClassNotFoundException e) {
			String msg = String.format("Class %s does not exist", msgType);
			logger.error(msg, e);
			throw new MessagingException(msg, e);
		} catch (IOException e) {
			String msg = "Failed to marshal the message payload";
			logger.error(msg, e);
			throw new MessagingException(msg, e);
		}

		return payload;
	}
}
