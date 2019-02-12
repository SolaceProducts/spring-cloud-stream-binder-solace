package com.solace.spring.cloud.stream.binder.properties;

import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;

public class SolaceBindingProperties implements BinderSpecificPropertiesProvider {

	private SolaceConsumerProperties consumer = new SolaceConsumerProperties();
	private SolaceProducerProperties producer = new SolaceProducerProperties();

	public SolaceConsumerProperties getConsumer() {
		return consumer;
	}

	public void setConsumer(SolaceConsumerProperties consumer) {
		this.consumer = consumer;
	}

	public SolaceProducerProperties getProducer() {
		return producer;
	}

	public void setProducer(SolaceProducerProperties producer) {
		this.producer = producer;
	}
}
