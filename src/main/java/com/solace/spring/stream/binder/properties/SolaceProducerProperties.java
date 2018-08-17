package com.solace.spring.stream.binder.properties;

public class SolaceProducerProperties extends SolaceCommonProperties {
	private Long msgTtl;
	private boolean msgDmqEligible = false;

	public Long getMsgTtl() {
		return msgTtl;
	}

	public void setMsgTtl(Long msgTtl) {
		this.msgTtl = msgTtl;
	}

	public boolean isMsgDmqEligible() {
		return msgDmqEligible;
	}

	public void setMsgDmqEligible(boolean msgDmqEligible) {
		this.msgDmqEligible = msgDmqEligible;
	}
}
