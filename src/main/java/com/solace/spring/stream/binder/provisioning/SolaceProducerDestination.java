package com.solace.spring.stream.binder.provisioning;

import org.springframework.cloud.stream.provisioning.ProducerDestination;

class SolaceProducerDestination implements ProducerDestination {
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
