package com.solace.spring.stream.binder.inbound;

import com.solace.spring.stream.binder.util.XMLMessageMapper;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.integration.support.AckUtils;
import org.springframework.integration.support.AcknowledgmentCallback;
import org.springframework.integration.support.StaticMessageHeaderAccessor;
import org.springframework.messaging.Message;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
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
		boolean isAcknowledged = false;
		Message<?> message = null;
		try {
			message = xmlMessageMapper.map(bytesXMLMessage);
			incrementDeliveryAttempt(message);
			messageConsumer.accept(message);
		} catch (RuntimeException e) {
			isAcknowledged = nack(message, bytesXMLMessage, isAcknowledged);
			if (errorHandlerFunction == null || ! errorHandlerFunction.apply(e)) {
				throw e;
			}
		}

		isAcknowledged = ack(message, bytesXMLMessage, isAcknowledged);
	}

	@Override
	public void onException(JCSMPException e) { //TODO Do we need anything here?
//		logger.warn("An unrecoverable error was received while listening for messages", e);
	}

	void incrementDeliveryAttempt(Message<?> message) {
		AtomicInteger deliveryAttempt = StaticMessageHeaderAccessor.getDeliveryAttempt(message);
		if (deliveryAttempt != null) {
			deliveryAttempt.incrementAndGet();
		}
	}

	boolean ack(Message<?> message, XMLMessage xmlMessage, boolean ackState) {
		if (ackState) return true;

		AcknowledgmentCallback ackCallback = getAckCallback(message);
		if (ackCallback != null) {
			AckUtils.autoAck(ackCallback);
		} else {
			xmlMessage.ackMessage();
		}

		return true;
	}

	boolean nack(Message<?> message, XMLMessage xmlMessage, boolean ackState) {
		if (ackState) return true;

		AcknowledgmentCallback ackCallback = getAckCallback(message);
		if (ackCallback != null) {
			AckUtils.autoNack(ackCallback);
		}

		return true;
	}

	private AcknowledgmentCallback getAckCallback(Message<?> message) {
		return message != null ? StaticMessageHeaderAccessor.getAcknowledgmentCallback(message) : null;
	}
}
