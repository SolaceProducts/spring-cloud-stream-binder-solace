package com.solace.spring.cloud.stream.binder.inbound;

import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPErrorResponseException;
import com.solacesystems.jcsmp.JCSMPErrorResponseSubcodeEx;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPFlowTransportUnsolicitedUnbindException;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.XMLMessageListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

class FlowConnectionScheduler {
	private final String queueName;
	private final JCSMPSession jcsmpSession;
	private final EndpointProperties endpointProperties;
	private final Consumer<Queue> onSuccess;
	private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

	private static final Log logger = LogFactory.getLog(FlowConnectionScheduler.class);

	FlowConnectionScheduler(String queueName, JCSMPSession jcsmpSession, EndpointProperties endpointProperties, Consumer<Queue> onSuccess) {
		this.queueName = queueName;
		this.jcsmpSession = jcsmpSession;
		this.endpointProperties = endpointProperties;
		this.onSuccess = onSuccess;
	}

	Future<FlowReceiver> createFutureFlow() {
		return createFutureFlow(null);

	}

	Future<FlowReceiver> createFutureFlow(XMLMessageListener xmlMessageListener) {
		final Callable<FlowReceiver> cxn = () -> {
			Queue queue = JCSMPFactory.onlyInstance().createQueue(queueName);
			final ConsumerFlowProperties flowProperties = new ConsumerFlowProperties();
			flowProperties.setEndpoint(queue);
			flowProperties.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

			FlowReceiver consumerFlowReceiver = null;
			boolean isWaiting = true;

			while (isWaiting) {
				try {
					consumerFlowReceiver = jcsmpSession.createFlow(xmlMessageListener, flowProperties, endpointProperties);
					consumerFlowReceiver.start();
					isWaiting = false;
				} catch (JCSMPException e) {
					if (isShutdownException(e)) {
						logger.info(String.format("Still waiting for connection to become available for queue %s", queueName));
						if (consumerFlowReceiver != null) {
							consumerFlowReceiver.close();
						}
					} else {
						String msg = String.format("Unable to get a %s for queue %s", FlowReceiver.class, queueName);
						logger.warn(msg, e);
						throw new JCSMPException(msg, e);
					}
				}

				if (isWaiting) Thread.sleep(1000);
			}

			if (onSuccess != null) {
				onSuccess.accept(queue);
			}

			return consumerFlowReceiver;
		};

		return scheduler.submit(cxn);
	}

	boolean isShutdownException(JCSMPException e) {
		boolean isQueueShutdown = e instanceof JCSMPErrorResponseException &&
				((JCSMPErrorResponseException) e).getResponseCode() == 503 &&
				((JCSMPErrorResponseException) e).getSubcodeEx() == JCSMPErrorResponseSubcodeEx.QUEUE_SHUTDOWN;
		return isQueueShutdown || e instanceof JCSMPFlowTransportUnsolicitedUnbindException;
	}

	void shutdown() {
		scheduler.shutdownNow();
	}
}
