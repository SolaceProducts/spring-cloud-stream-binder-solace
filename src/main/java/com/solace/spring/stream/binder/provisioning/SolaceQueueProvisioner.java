package com.solace.spring.stream.binder.provisioning;

import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.JCSMPErrorResponseException;
import com.solacesystems.jcsmp.JCSMPErrorResponseSubcodeEx;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.Topic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import com.solace.spring.stream.binder.properties.SolaceConsumerProperties;
import com.solace.spring.stream.binder.properties.SolaceProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.util.StringUtils;

public class SolaceQueueProvisioner
		implements ProvisioningProvider<ExtendedConsumerProperties<SolaceConsumerProperties>, ExtendedProducerProperties<SolaceProducerProperties>> {

	private JCSMPSession jcsmpSession;

	private static final Log logger = LogFactory.getLog(SolaceQueueProvisioner.class);

	public SolaceQueueProvisioner(JCSMPSession jcsmpSession) {
		this.jcsmpSession = jcsmpSession;
	}

	@Override
	public ProducerDestination provisionProducerDestination(String name, ExtendedProducerProperties<SolaceProducerProperties> properties) throws ProvisioningException {
		String topicName = name; //TODO Any fancy transforms

		for (String groupName : properties.getRequiredGroups()) {
			String baseQueueName = properties.getExtension().isQueueNameGroupOnly() ? groupName : topicName + "." + groupName;

			//TODO Make Configurable
			EndpointProperties endpointProperties = new EndpointProperties();
			endpointProperties.setPermission(EndpointProperties.PERMISSION_DELETE);
			endpointProperties.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);
			endpointProperties.setQuota(1500);

			if (properties.isPartitioned()) { //TODO

			} else {
				Queue queue = provisionQueue(baseQueueName, true);
				addSubscriptionToQueue(queue, topicName);
			}
		}
		return new SolaceProducerDestination(topicName);
	}

	@Override
	public ConsumerDestination provisionConsumerDestination(String name, String group, ExtendedConsumerProperties<SolaceConsumerProperties> properties) throws ProvisioningException {
		//TODO Do anonymous endpoints when no group is given like RabbitMQ?

		String topicName = name; //TODO Any fancy transforms

		String baseQueueName = topicName + "." + (StringUtils.hasText(group) ? group : "default");
		String queueName = baseQueueName; //TODO Any fancy transforms
		if (properties.isPartitioned()) { //TODO Partitioning

		}
		Queue queue = provisionQueue(queueName, properties.getExtension().isDurableQueue());
		addSubscriptionToQueue(queue, topicName);
		return new SolaceConsumerDestination(queue.getName());
	}

	private Queue provisionQueue(String name, boolean isDurable) throws ProvisioningException {
		//TODO Parameterize this?
		EndpointProperties endpointProperties = new EndpointProperties();
		endpointProperties.setPermission(EndpointProperties.PERMISSION_DELETE);
		endpointProperties.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);
		endpointProperties.setQuota(1500);

		Queue queue;
		if (isDurable) {
			queue = JCSMPFactory.onlyInstance().createQueue(name);
		} else {
			try {
				queue = jcsmpSession.createTemporaryQueue(name);
			} catch (JCSMPException e) {
				throw new ProvisioningException(String.format("Failed to create non-durable queue %s", name), e);
			}
		}

		try {
			jcsmpSession.provision(queue, endpointProperties, JCSMPSession.FLAG_IGNORE_ALREADY_EXISTS);
		} catch (JCSMPException e) {
			String msg = String.format("Failed to provision queue %s", name);
			logger.error(msg, e);
			throw new ProvisioningException(msg, e);
		}

		return queue;
	}

	private void addSubscriptionToQueue(Queue queue, String topicName) {
		logger.info(String.format("Subscribing queue %s to topic %s", queue.getName(), topicName));
		try {
			Topic topic = JCSMPFactory.onlyInstance().createTopic(topicName);
			try {
				jcsmpSession.addSubscription(queue, topic, JCSMPSession.WAIT_FOR_CONFIRM);
			} catch (JCSMPErrorResponseException e) {
				if (e.getSubcodeEx() == JCSMPErrorResponseSubcodeEx.SUBSCRIPTION_ALREADY_PRESENT) {
					logger.warn(String.format(
							"Queue %s is already subscribed to topic %s, SUBSCRIPTION_ALREADY_PRESENT error will be ignored...",
							queue.getName(), topicName));
				} else {
					throw e;
				}
			}
		} catch (JCSMPException e) {
			String msg = String.format("Failed to add subscription of %s to queue %s", topicName, queue.getName());
			logger.error(msg, e);
			throw new ProvisioningException(msg, e);
		}
	}

	private static final class SolaceProducerDestination implements ProducerDestination {
		private String topicEndpointName;

		SolaceProducerDestination(String topicEndpointName) {
			this.topicEndpointName = topicEndpointName;
		}

		@Override
		public String getName() {
			return topicEndpointName;
		}

		@Override
		public String getNameForPartition(int partition) {
			return topicEndpointName;
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("SolaceProducerDestination{");
			sb.append("topicEndpointName='").append(topicEndpointName).append('\'');
			sb.append('}');
			return sb.toString();
		}
	}

	private static final class SolaceConsumerDestination implements ConsumerDestination {
		private String queueName;

		public SolaceConsumerDestination(String queueName) {
			this.queueName = queueName;
		}

		@Override
		public String getName() {
			return queueName;
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("SolaceConsumerDestination{");
			sb.append("queueName='").append(queueName).append('\'');
			sb.append('}');
			return sb.toString();
		}
	}
}
