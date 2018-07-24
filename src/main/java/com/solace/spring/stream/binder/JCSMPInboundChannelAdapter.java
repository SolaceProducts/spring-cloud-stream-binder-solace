package com.solace.spring.stream.binder;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.XMLMessageListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.messaging.MessagingException;

class JCSMPInboundChannelAdapter extends MessageProducerSupport implements OrderlyShutdownCapable {
	private String queueName;
	private JCSMPSession jcsmpSession;
	private FlowReceiver consumerFlowReceiver;
	private XMLMessageListener listener = new InboundXMLMessageListener();
	private XMLMessageMapper xmlMessageMapper = new XMLMessageMapper();

	private static final Log logger = LogFactory.getLog(JCSMPInboundChannelAdapter.class);

	public JCSMPInboundChannelAdapter(ConsumerDestination consumerDestination, JCSMPSession jcsmpSession) {
		this.queueName = consumerDestination.getName();
		this.jcsmpSession = jcsmpSession;
	}

	@Override
	protected void doStart() {
		try {
			logger.info(String.format("Creating consumer flow for queue %s", queueName));
			final ConsumerFlowProperties flowProperties = new ConsumerFlowProperties();
			flowProperties.setEndpoint(JCSMPFactory.onlyInstance().createQueue(queueName));
			flowProperties.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_AUTO);
			consumerFlowReceiver = jcsmpSession.createFlow(listener, flowProperties);
			consumerFlowReceiver.start();
		} catch (JCSMPException e) {
			String msg = "Failed to get message consumer from session";
			logger.error(msg, e);
			throw new MessagingException(msg, e);
		}
	}

	@Override
	protected void doStop() {
		consumerFlowReceiver.stop();
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

	private class InboundXMLMessageListener implements XMLMessageListener {

		@Override
		public void onReceive(BytesXMLMessage bytesXMLMessage) {
			//TODO Any headers?
			Object payload = xmlMessageMapper.map(bytesXMLMessage);
			sendMessage(new DefaultMessageBuilderFactory().withPayload(payload).build());
		}

		@Override
		public void onException(JCSMPException e) {
			logger.error("An unrecoverable error was received while listening for messages", e);
		}
	}
}
