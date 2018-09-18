package com.solace.spring.cloud.stream.binder.inbound;

import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.XMLMessageListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.lang.Nullable;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class JCSMPInboundChannelAdapter extends MessageProducerSupport implements OrderlyShutdownCapable {
	private final String id = UUID.randomUUID().toString();
	private final ConsumerDestination consumerDestination;
	private RetryTemplate retryTemplate;
	private RecoveryCallback<?> recoveryCallback;

	private final FlowConnectionScheduler flowConnectionScheduler;
	private FlowReceiver flowReceiver;

	private static final Log logger = LogFactory.getLog(JCSMPInboundChannelAdapter.class);
	private static final ThreadLocal<AttributeAccessor> attributesHolder = new ThreadLocal<>();

	public JCSMPInboundChannelAdapter(ConsumerDestination consumerDestination, JCSMPSession jcsmpSession,
									  ExtendedConsumerProperties<SolaceConsumerProperties> consumerProperties,
									  @Nullable EndpointProperties endpointProperties, @Nullable Consumer<Queue> postStart) {
		this.consumerDestination = consumerDestination;
		this.flowConnectionScheduler = new FlowConnectionScheduler(consumerDestination.getName(), jcsmpSession,
				endpointProperties, postStart, consumerProperties.getExtension().getQueueReconnectRetryWaitInMillis());
	}

	@Override
	protected void doStart() {
		final String queueName = consumerDestination.getName();
		logger.info(String.format("Creating consumer flow for queue %s <inbound adapter %s>", queueName, id));

		if (isRunning()) {
			logger.warn(String.format("Nothing to do. Inbound message channel adapter %s is already running", id));
			return;
		}

		try {
			flowReceiver = flowConnectionScheduler.createFutureFlow(buildListener()).get();
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		} catch (InterruptedException ignored) {}
	}

	@Override
	protected void doStop() {
		if (!isRunning()) return;
		flowConnectionScheduler.shutdown();
		final String queueName = consumerDestination.getName();
		logger.info(String.format("Stopping consumer flow from queue %s <inbound adapter ID: %s>", queueName, id));
		flowReceiver.close();
	}

	@Override
	public int beforeShutdown() {
		this.stop();
		return 0;
	}

	@Override
	public int afterShutdown() {
		return 0;
	}

	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	public void setRecoveryCallback(RecoveryCallback<?> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	@Override
	protected AttributeAccessor getErrorMessageAttributes(org.springframework.messaging.Message<?> message) {
		AttributeAccessor attributes = attributesHolder.get();
		return attributes == null ? super.getErrorMessageAttributes(message) : attributes;
	}

	private XMLMessageListener buildListener() {
		Consumer<FlowReceiver> reconnectHandler = (flow) -> {
			flowReceiver.close();
			logger.info(String.format("Re-established connection to queue %s", consumerDestination.getName()));
			flowReceiver = flow;
		};

		XMLMessageListener listener;
		if (retryTemplate != null) {
			Assert.state(getErrorChannel() == null,
					"Cannot have an 'errorChannel' property when a 'RetryTemplate' is provided; " +
							"use an 'ErrorMessageSendingRecoverer' in the 'recoveryCallback' property to send " +
							"an error message when retries are exhausted");
			RetryableInboundXMLMessageListener retryableMessageListener = new RetryableInboundXMLMessageListener(
					consumerDestination,
					this::sendMessage,
					flowConnectionScheduler,
					reconnectHandler,
					(exception) -> sendErrorMessageIfNecessary(null, exception),
					retryTemplate,
					recoveryCallback,
					attributesHolder
			);
			retryTemplate.registerListener(retryableMessageListener);
			listener = retryableMessageListener;
		} else {
			listener = new InboundXMLMessageListener(
					consumerDestination,
					this::sendMessage,
					flowConnectionScheduler,
					reconnectHandler,
					(exception) -> sendErrorMessageIfNecessary(null, exception),
					attributesHolder,
					this.getErrorChannel() != null
			);
		}
		return listener;
	}
}
