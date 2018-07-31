package com.solace.spring.stream.binder;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.XMLMessageListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
import java.util.function.Function;

class InboundXMLMessageListener implements XMLMessageListener {
	final XMLMessageMapper xmlMessageMapper = new XMLMessageMapper();
	final ConsumerDestination consumerDestination;
	final Consumer<Message<?>> messageConsumer;
	private final Function<RuntimeException,Boolean> errorHandlerFunction;

	private static final Log logger = LogFactory.getLog(InboundXMLMessageListener.class);

	InboundXMLMessageListener(ConsumerDestination consumerDestination,
							  Consumer<Message<?>> messageConsumer,
							  Function<RuntimeException,Boolean> errorHandlerFunction) {
		this.consumerDestination = consumerDestination;
		this.messageConsumer = messageConsumer;
		this.errorHandlerFunction = errorHandlerFunction;
	}

	@Override
	public void onReceive(BytesXMLMessage bytesXMLMessage) {
		Message<?> message = xmlMessageMapper.map(bytesXMLMessage);

		try {
			messageConsumer.accept(message);
		} catch (RuntimeException e) {
			if (errorHandlerFunction == null || ! errorHandlerFunction.apply(e)) {
				throw e;
			}
		}
	}

	@Override
	public void onException(JCSMPException e) { //TODO Do we need anything here?
//		logger.error("An unrecoverable error was received while listening for messages", e);
	}
}
