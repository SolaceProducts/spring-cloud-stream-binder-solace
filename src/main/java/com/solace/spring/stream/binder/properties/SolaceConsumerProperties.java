package com.solace.spring.stream.binder.properties;

public class SolaceConsumerProperties extends SolaceCommonProperties {
	private String anonymousGroupPrefix = "anon";
	private int polledConsumerWaitTimeInMillis = 500;

	public String getAnonymousGroupPrefix() {
		return anonymousGroupPrefix;
	}

	public void setAnonymousGroupPrefix(String anonymousGroupPrefix) {
		this.anonymousGroupPrefix = anonymousGroupPrefix;
	}

	public int getPolledConsumerWaitTimeInMillis() {
		return polledConsumerWaitTimeInMillis;
	}

	public void setPolledConsumerWaitTimeInMillis(int polledConsumerWaitTimeInMillis) {
		this.polledConsumerWaitTimeInMillis = polledConsumerWaitTimeInMillis;
	}
}
