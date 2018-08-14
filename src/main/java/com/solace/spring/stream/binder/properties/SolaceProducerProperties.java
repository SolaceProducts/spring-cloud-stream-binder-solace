package com.solace.spring.stream.binder.properties;

public class SolaceProducerProperties extends SolaceCommonProperties {
	private Long msgTimeToLive;
	private boolean dmqEligible = false;

	public Long getMsgTimeToLive() {
		return msgTimeToLive;
	}

	public void setMsgTimeToLive(Long msgTimeToLive) {
		this.msgTimeToLive = msgTimeToLive;
	}

	public boolean isDmqEligible() {
		return dmqEligible;
	}

	public void setDmqEligible(boolean dmqEligible) {
		this.dmqEligible = dmqEligible;
	}
}
