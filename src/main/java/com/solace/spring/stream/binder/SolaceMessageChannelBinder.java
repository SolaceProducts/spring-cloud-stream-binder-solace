package com.solace.spring.stream.binder;

import com.solace.spring.stream.binder.util.SolaceProvisioningUtil;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedPropertiesBinder;
import com.solace.spring.stream.binder.properties.SolaceConsumerProperties;
import com.solace.spring.stream.binder.properties.SolaceExtendedBindingProperties;
import com.solace.spring.stream.binder.properties.SolaceProducerProperties;
import com.solace.spring.stream.binder.provisioning.SolaceQueueProvisioner;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

public class SolaceMessageChannelBinder
		extends AbstractMessageChannelBinder<
						ExtendedConsumerProperties<SolaceConsumerProperties>,
						ExtendedProducerProperties<SolaceProducerProperties>,
						SolaceQueueProvisioner>
		implements ExtendedPropertiesBinder<MessageChannel, SolaceConsumerProperties, SolaceProducerProperties>,
				DisposableBean {

	private JCSMPSession jcsmpSession;
	private SolaceExtendedBindingProperties extendedBindingProperties = new SolaceExtendedBindingProperties();
	private JCSMPSessionProducerManager sessionProducerManager;

	public SolaceMessageChannelBinder(JCSMPSession jcsmpSession, SolaceQueueProvisioner solaceQueueProvisioner) {
		super(new String[0], solaceQueueProvisioner);
		this.jcsmpSession = jcsmpSession;
		this.sessionProducerManager = new JCSMPSessionProducerManager(jcsmpSession);
	}

	@Override
	public void destroy() throws Exception {
		jcsmpSession.closeSession();
	}

	@Override
	protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
														  ExtendedProducerProperties<SolaceProducerProperties> producerProperties,
														  MessageChannel errorChannel)
			throws Exception {

		//TODO Handle ERROR Channel
		return new JCSMPOutboundMessageHandler(destination, jcsmpSession, errorChannel, sessionProducerManager);
	}

	@Override
	protected MessageProducer createConsumerEndpoint(ConsumerDestination destination, String group,
													 ExtendedConsumerProperties<SolaceConsumerProperties> properties)
			throws Exception {

		// WORKAROUND (SOL-4272) ----------------------------------------------------------
		// Temporary endpoints are only provisioned when the consumer is created.
		// Ideally, these should be done within the provisioningProvider itself.
		EndpointProperties endpointProperties = null;
		Runnable postStart = null;

		if (! SolaceProvisioningUtil.isDurableQueue(group, properties.getExtension())) {
			endpointProperties = SolaceProvisioningUtil.getEndpointProperties(properties.getExtension());

			postStart = () -> {
				String queueName = destination.getName();
				String topicName = provisioningProvider.getBoundTopicNameForQueue(queueName);
				Queue queueReference = JCSMPFactory.onlyInstance().createQueue(queueName);
				provisioningProvider.addSubscriptionToQueue(queueReference, topicName);
			};
		}
		// --------------------------------------------------------------------------------

		return new JCSMPInboundChannelAdapter(destination, jcsmpSession, endpointProperties, postStart);
	}

	@Override
	public SolaceConsumerProperties getExtendedConsumerProperties(String channelName) {
		return extendedBindingProperties.getExtendedConsumerProperties(channelName);
	}

	@Override
	public SolaceProducerProperties getExtendedProducerProperties(String channelName) {
		return extendedBindingProperties.getExtendedProducerProperties(channelName);
	}

	public void setExtendedBindingProperties(SolaceExtendedBindingProperties extendedBindingProperties) {
		this.extendedBindingProperties = extendedBindingProperties;
	}
}
