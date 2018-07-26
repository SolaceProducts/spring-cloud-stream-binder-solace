package com.solace.spring.stream.binder.properties;

import com.solacesystems.jcsmp.EndpointProperties;

public class SolaceCommonProperties {
	private boolean isQueueNameGroupOnly = false;
	private String prefix = "";
	private Integer queueDiscardBehaviour = null;
	private Integer queueMaxMsgRedelivery = null;
	private Integer queueMaxMsgSize = null;
	private int queuePermission = EndpointProperties.PERMISSION_CONSUME;
	private Integer queueQuota = null;
	private Boolean isRespectsMsgTTL = null;

	public boolean isQueueNameGroupOnly() {
		return isQueueNameGroupOnly;
	}

	public void setQueueNameGroupOnly(boolean queueNameGroupOnly) {
		isQueueNameGroupOnly = queueNameGroupOnly;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Integer getQueueDiscardBehaviour() {
		return queueDiscardBehaviour;
	}

	public void setQueueDiscardBehaviour(Integer queueDiscardBehaviour) {
		this.queueDiscardBehaviour = queueDiscardBehaviour;
	}

	public Integer getQueueMaxMsgRedelivery() {
		return queueMaxMsgRedelivery;
	}

	public void setQueueMaxMsgRedelivery(Integer queueMaxMsgRedelivery) {
		this.queueMaxMsgRedelivery = queueMaxMsgRedelivery;
	}

	public Integer getQueueMaxMsgSize() {
		return queueMaxMsgSize;
	}

	public void setQueueMaxMsgSize(Integer queueMaxMsgSize) {
		this.queueMaxMsgSize = queueMaxMsgSize;
	}

	public int getQueuePermission() {
		return queuePermission;
	}

	public void setQueuePermission(int queuePermission) {
		this.queuePermission = queuePermission;
	}

	public Integer getQueueQuota() {
		return queueQuota;
	}

	public void setQueueQuota(Integer queueQuota) {
		this.queueQuota = queueQuota;
	}

	public Boolean getRespectsMsgTTL() {
		return isRespectsMsgTTL;
	}

	public void setRespectsMsgTTL(Boolean respectsMsgTTL) {
		isRespectsMsgTTL = respectsMsgTTL;
	}
}
