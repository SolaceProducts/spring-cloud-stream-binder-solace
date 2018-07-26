package com.solace.spring.stream.binder.properties;

public class SolaceConsumerProperties extends SolaceCommonProperties {
	private String anonymousGroupPrefix = "anon";

	public String getAnonymousGroupPrefix() {
		return anonymousGroupPrefix;
	}

	public void setAnonymousGroupPrefix(String anonymousGroupPrefix) {
		this.anonymousGroupPrefix = anonymousGroupPrefix;
	}
}
