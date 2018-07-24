package com.solace.spring.stream.binder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.solacesystems.common.util.Pair;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class JCSMPOutboundMessageHandler implements MessageHandler, Lifecycle {
	private final String id = UUID.randomUUID().toString();
	private final JCSMPSession jcsmpSession;
	private final Topic topic;
	private MessageChannel errorChannel;
	private XMLMessageProducer producer;
	private boolean isRunning = false;
	private XMLMessageMapper xmlMessageMapper = new XMLMessageMapper();

	private static final Log logger = LogFactory.getLog(JCSMPOutboundMessageHandler.class);
	private static SessionProducerManager sessionProducerManager = new SessionProducerManager();

	JCSMPOutboundMessageHandler(ProducerDestination destination, JCSMPSession jcsmpSession, MessageChannel errorChannel) {
		this.jcsmpSession = jcsmpSession;
		this.errorChannel = errorChannel;
		this.topic = JCSMPFactory.onlyInstance().createTopic(destination.getName());
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
			logger.info("Nothing to do, this message handler is already running");
			return;
		}

		try {
			producer = sessionProducerManager.getProducer(id, jcsmpSession, topic);
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
		sessionProducerManager.stopProducer(id, jcsmpSession);
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	private static class SessionProducerManager {
		private Map<String, Pair<XMLMessageProducer, Set<String>>> sessionMessageProducer = new HashMap<>();
		private Map<String, Object> sessionLocks = new HashMap<>();

		XMLMessageProducer getProducer(String handlerId, JCSMPSession jcsmpSession, Topic topic) throws JCSMPException {
			final String sessionName = jcsmpSession.getSessionName();
			XMLMessageProducer producer;

			synchronized (getSessionLock(sessionName)) {
				if (!sessionMessageProducer.containsKey(sessionName)) {
					logger.info(String.format("No message producer exists for session %s, a new one will be created", sessionName));
					producer = jcsmpSession.getMessageProducer(publisherEventHandler);
					sessionMessageProducer.put(sessionName, new Pair<>(producer, new HashSet<>()));
				} else {
					logger.info(String.format(
							"A message producer already exists for session %s, reusing it to send messages to topic %s",
							sessionName, topic.getName()));
					producer = sessionMessageProducer.get(sessionName).getFirst();
				}

				sessionMessageProducer.get(sessionName).getSecond().add(handlerId);
			}

			return producer;
		}

		void stopProducer(String handlerId, JCSMPSession jcsmpSession) {
			final String sessionName = jcsmpSession.getSessionName();
			synchronized (getSessionLock(sessionName)) {
				Set<String> sessionMessageHandlers = sessionMessageProducer.get(sessionName).getSecond();
				if (sessionMessageHandlers.contains(handlerId) && sessionMessageHandlers.size() == 1) {
					logger.info(String.format("%s is the last message handler for session %s, closing producer...",
							handlerId, sessionName));
					sessionMessageProducer.get(sessionName).getFirst().close();
					sessionMessageProducer.remove(sessionName);
				} else {
					logger.info(String.format("%s is not the last message handler for session %s, persisting producer...",
							handlerId, sessionName));
					sessionMessageHandlers.remove(handlerId);
				}
			}
		}

		private Object getSessionLock(String sessionName) {
			if (! sessionLocks.containsKey(sessionName)) sessionLocks.put(sessionName, new Object());
			return sessionLocks.get(sessionName);
		}

		//TODO Make this a configurable property?
		private JCSMPStreamingPublishEventHandler publisherEventHandler = new JCSMPStreamingPublishEventHandler() {
			@Override
			public void responseReceived(String messageID) {
				logger.info("Producer received response for msg: " + messageID);
			}

			@Override
			public void handleError(String messageID, JCSMPException e, long timestamp) {
				logger.error("Producer received error for msg: " + messageID + " - " + timestamp, e);
			}
		};
	}
}
