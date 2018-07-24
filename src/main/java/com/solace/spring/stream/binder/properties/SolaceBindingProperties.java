package com.solace.spring.stream.binder.properties;

public class SolaceBindingProperties {

	private SolaceConsumerProperties consumerProperties = new SolaceConsumerProperties();
	private SolaceProducerProperties producerProperties = new SolaceProducerProperties();

	public SolaceConsumerProperties getConsumerProperties() {
		return consumerProperties;
	}

	public void setConsumerProperties(SolaceConsumerProperties consumerProperties) {
		this.consumerProperties = consumerProperties;
	}

	public SolaceProducerProperties getProducerProperties() {
		return producerProperties;
	}

	public void setProducerProperties(SolaceProducerProperties producerProperties) {
		this.producerProperties = producerProperties;
	}
}
