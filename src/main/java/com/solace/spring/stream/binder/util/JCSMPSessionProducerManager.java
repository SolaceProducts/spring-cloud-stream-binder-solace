package com.solace.spring.stream.binder.util;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.XMLMessageProducer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Set;

public class JCSMPSessionProducerManager {
	private final JCSMPSession session;
	private XMLMessageProducer producer;
	private final Object lock = new Object();
	private Set<String> messageHandlers = new HashSet<>();

	private static final Log logger = LogFactory.getLog(JCSMPSessionProducerManager.class);

	public JCSMPSessionProducerManager(JCSMPSession session) {
		this.session = session;
	}

	public XMLMessageProducer getProducer(String messageHandlerId) throws JCSMPException {
		synchronized (lock) {
			if (messageHandlers.isEmpty()) {
				logger.info(String.format("No message producer exists for session %s, a new one will be created",
						session.getSessionName()));
				producer = session.getMessageProducer(publisherEventHandler);
			} else {
				logger.info(String.format(
						"A message producer already exists for session %s, reusing it",
						session.getSessionName()));
			}

			messageHandlers.add(messageHandlerId);
		}
		return producer;
	}

	public void closeProducer(String messageHandlerId) {
		synchronized (lock) {
			if (messageHandlers.contains(messageHandlerId) && messageHandlers.size() <= 1) {
				logger.info(String.format("%s is the last message handler for session %s, closing producer...",
						messageHandlerId, session.getSessionName()));
				producer.close();
				producer = null;
			} else {
				logger.info(String.format("%s is not the last message handler for session %s, persisting producer...",
						messageHandlerId, session.getSessionName()));
			}
			messageHandlers.remove(messageHandlerId);
		}
	}

	//TODO Make this a configurable property?
	private JCSMPStreamingPublishEventHandler publisherEventHandler = new JCSMPStreamingPublishEventHandler() {
		@Override
		public void responseReceived(String messageID) {
			logger.debug("Producer received response for msg: " + messageID);
		}

		@Override
		public void handleError(String messageID, JCSMPException e, long timestamp) {
			logger.error("Producer received error for msg: " + messageID + " - " + timestamp, e);
		}
	};
}
