package com.solace.spring.stream.binder.properties;

public class SolaceConsumerProperties extends SolaceCommonProperties {
	private boolean isDurableQueue = true;

	public boolean isDurableQueue() {
		return isDurableQueue;
	}

	public SolaceConsumerProperties setDurableQueue(boolean durableQueue) {
		isDurableQueue = durableQueue;
		return this;
	}
}
