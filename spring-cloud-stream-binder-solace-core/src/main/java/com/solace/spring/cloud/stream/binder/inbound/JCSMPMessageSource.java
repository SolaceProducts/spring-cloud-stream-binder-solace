package com.solace.spring.cloud.stream.binder.inbound;

import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;
import com.solace.spring.cloud.stream.binder.util.ClosedChannelBindingException;
import com.solace.spring.cloud.stream.binder.util.XMLMessageMapper;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.messaging.MessagingException;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class JCSMPMessageSource extends AbstractMessageSource<Object> implements Lifecycle {
	private final String id = UUID.randomUUID().toString();
	private final String queueName;
	private ExtendedConsumerProperties<SolaceConsumerProperties> consumerProperties;
	private final XMLMessageMapper xmlMessageMapper = new XMLMessageMapper();

	private final FlowConnectionScheduler flowConnectionScheduler;
	private Future<FlowReceiver> futureFlowReceiver;
	private FlowReceiver flowReceiver;

	private boolean isRunning = false;

	public JCSMPMessageSource(ConsumerDestination destination,
							  JCSMPSession jcsmpSession,
							  ExtendedConsumerProperties<SolaceConsumerProperties> consumerProperties,
							  EndpointProperties endpointProperties,
							  Consumer<Queue> postStart) {
		this.queueName = destination.getName();
		this.consumerProperties = consumerProperties;
		this.flowConnectionScheduler = new FlowConnectionScheduler(destination.getName(), jcsmpSession,
				endpointProperties, postStart);
	}

	@Override
	protected Object doReceive() {
		if (!isRunning()) {
			String msg0 = String.format("Cannot receive message using message source %s", id);
			String msg1 = String.format("Message source %s is not running", id);
			ClosedChannelBindingException closedBindingException = new ClosedChannelBindingException(msg1);
			logger.warn(msg0, closedBindingException);
			throw new MessagingException(msg0, closedBindingException);
		}

		if (futureFlowReceiver != null) {
			if (!futureFlowReceiver.isDone()) {
				return null;
			}

			try {
				flowReceiver = futureFlowReceiver.get();
				futureFlowReceiver = null;
			} catch (ExecutionException e) {
				throw new RuntimeException(e.getCause());
			} catch (InterruptedException ignored) {}
		}

		BytesXMLMessage xmlMessage;
		try {
			int timeout = consumerProperties.getExtension().getPolledConsumerWaitTimeInMillis();
			xmlMessage = flowReceiver.receive(timeout);
		} catch (JCSMPException e) {
			if (!isRunning()) {
				logger.warn(String.format("Exception received while consuming a message, but the consumer " +
						"<message source ID: %s> is currently shutdown. Exception will be ignored", id), e);
				return null;
			} else if (flowConnectionScheduler.isShutdownException(e)) {
				logger.info(String.format("Queue %s was shutdown. Starting reconnect loop...", queueName));
				flowReceiver.close();
				flowReceiver = null;
				futureFlowReceiver = flowConnectionScheduler.createFutureFlow();
				return null;
			} else {
				String msg = String.format("Unable to consume message from queue %s", queueName);
				logger.warn(msg, e);
				throw new MessagingException(msg, e);
			}
		}

		return xmlMessage != null ? xmlMessageMapper.map(xmlMessage, true) : null;
	}

	@Override
	public String getComponentType() {
		return "jcsmp:message-source";
	}

	@Override
	public void start() {
		logger.info(String.format("Creating consumer to queue %s <message source ID: %s>", queueName, id));
		if (isRunning()) {
			logger.warn(String.format("Nothing to do, message source %s is already running", id));
			return;
		}

		futureFlowReceiver = flowConnectionScheduler.createFutureFlow();
		isRunning = true;
	}

	@Override
	public void stop() {
		if (!isRunning()) return;
		flowConnectionScheduler.shutdown();
		logger.info(String.format("Stopping consumer to queue %s <message source ID: %s>", queueName, id));
		flowReceiver.close();
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}
}
