package com.solace.spring.stream.binder.properties;

public class SolaceCommonProperties {
	private boolean isDurableTopicEndpoint = true;

	public boolean isDurableTopicEndpoint() {
		return isDurableTopicEndpoint;
	}

	public SolaceCommonProperties setDurableTopicEndpoint(boolean durableTopicEndpoint) {
		isDurableTopicEndpoint = durableTopicEndpoint;
		return this;
	}
}
