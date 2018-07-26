package com.solace.spring.stream.binder.properties;

public class SolaceConsumerProperties extends SolaceCommonProperties {
	private String anonymousGroupPrefix = "anon";
	private boolean isNonRequiredQueueDurable = true;

	public String getAnonymousGroupPrefix() {
		return anonymousGroupPrefix;
	}

	public void setAnonymousGroupPrefix(String anonymousGroupPrefix) {
		this.anonymousGroupPrefix = anonymousGroupPrefix;
	}

	public boolean isNonRequiredQueueDurable() {
		return isNonRequiredQueueDurable;
	}

	public void setNonRequiredQueueDurable(boolean nonRequiredQueueDurable) {
		isNonRequiredQueueDurable = nonRequiredQueueDurable;
	}
}
