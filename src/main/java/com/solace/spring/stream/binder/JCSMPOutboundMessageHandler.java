package com.solace.spring.stream.binder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageProducer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.context.Lifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import java.util.UUID;

class JCSMPOutboundMessageHandler implements MessageHandler, Lifecycle {
	private final String id = UUID.randomUUID().toString();
	private final Topic topic;
	private final JCSMPSession jcsmpSession;
	private MessageChannel errorChannel;
	private JCSMPSessionProducerManager sessionProducerManager;
	private XMLMessageProducer producer;
	private XMLMessageMapper xmlMessageMapper = new XMLMessageMapper();
	private boolean isRunning = false;

	private static final Log logger = LogFactory.getLog(JCSMPOutboundMessageHandler.class);

	JCSMPOutboundMessageHandler(ProducerDestination destination, JCSMPSession jcsmpSession, MessageChannel errorChannel,
								JCSMPSessionProducerManager sessionProducerManager) {
		this.topic = JCSMPFactory.onlyInstance().createTopic(destination.getName());
		this.jcsmpSession = jcsmpSession;
		this.errorChannel = errorChannel;
		this.sessionProducerManager = sessionProducerManager;
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		if (! isRunning) {
			String msg = String.format("Cannot send message, message handler %s is not running", id);
			if (errorChannel != null) errorChannel.send(message);
			logger.error(msg);
			throw new MessagingException(msg);
		}

		XMLMessage xmlMessage;
		try {
			xmlMessage = xmlMessageMapper.map(message.getPayload(), message.getPayload().getClass());
			xmlMessage.setDeliveryMode(DeliveryMode.PERSISTENT);
		} catch (JsonProcessingException e) {
			String msg = "Failed to serialize payload";
			if (errorChannel != null) errorChannel.send(message);
			logger.error(msg, e);
			throw new MessagingException(msg, e);
		}

		try {
			producer.send(xmlMessage, topic);
		} catch (JCSMPException e) {
			String msg = String.format("Unable to send message to topic %s", topic.getName());
			if (errorChannel != null) errorChannel.send(message);
			logger.error(msg, e);
			throw new MessagingException(msg, e);
		}
	}

	@Override
	public void start() {
		logger.info(String.format("Creating producer to topic %s <message handler ID: %s>", topic.getName(), id));
		if (isRunning()) {
			logger.warn(String.format("Nothing to do, message handler %s is already running", id));
			return;
		}

		try {
			producer = sessionProducerManager.getProducer(id);
		} catch (JCSMPException e) {
			String msg = String.format("Unable to get a message producer for session %s", jcsmpSession.getSessionName());
			logger.error(msg, e);
			throw new RuntimeException(msg, e);
		}

		isRunning = true;
	}

	@Override
	public void stop() {
		logger.info(String.format("Stopping producer to topic %s <message handler ID: %s>", topic.getName(), id));
		sessionProducerManager.closeProducer(id);
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}
}
