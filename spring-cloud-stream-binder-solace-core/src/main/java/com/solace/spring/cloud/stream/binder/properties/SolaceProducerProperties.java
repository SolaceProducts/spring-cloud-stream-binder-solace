package com.solace.spring.cloud.stream.binder.properties;

public class SolaceProducerProperties extends SolaceCommonProperties {
	private Long msgTtl = null;
	private boolean msgInternalDmqEligible = false;

	public Long getMsgTtl() {
		return msgTtl;
	}

	public void setMsgTtl(Long msgTtl) {
		this.msgTtl = msgTtl;
	}

	public boolean isMsgInternalDmqEligible() {
		return msgInternalDmqEligible;
	}

	public void setMsgInternalDmqEligible(boolean msgInternalDmqEligible) {
		this.msgInternalDmqEligible = msgInternalDmqEligible;
	}
}
