package com.solace.spring.stream.binder.properties;

import com.solacesystems.jcsmp.EndpointProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cloud.stream.solace.binder")
public class SolaceBinderConfigurationProperties {

	// DMQ Properties ---------
	private boolean dmqEnabled = false; // DMQ Name: #DEAD_MSG_QUEUE
	private int dmqAccessType = EndpointProperties.ACCESSTYPE_NONEXCLUSIVE;
	private int dmqPermission = EndpointProperties.PERMISSION_CONSUME;
	private Integer dmqDiscardBehaviour = null;
	private Integer dmqMaxMsgRedelivery = null;
	private Integer dmqMaxMsgSize = null;
	private Integer dmqQuota = null;
	private Boolean dmqRespectsMsgTTL = null;
	// ------------------------

	public boolean isDmqEnabled() {
		return dmqEnabled;
	}

	public void setDmqEnabled(boolean dmqEnabled) {
		this.dmqEnabled = dmqEnabled;
	}

	public int getDmqAccessType() {
		return dmqAccessType;
	}

	public void setDmqAccessType(int dmqAccessType) {
		this.dmqAccessType = dmqAccessType;
	}

	public int getDmqPermission() {
		return dmqPermission;
	}

	public void setDmqPermission(int dmqPermission) {
		this.dmqPermission = dmqPermission;
	}

	public Integer getDmqDiscardBehaviour() {
		return dmqDiscardBehaviour;
	}

	public void setDmqDiscardBehaviour(Integer dmqDiscardBehaviour) {
		this.dmqDiscardBehaviour = dmqDiscardBehaviour;
	}

	public Integer getDmqMaxMsgRedelivery() {
		return dmqMaxMsgRedelivery;
	}

	public void setDmqMaxMsgRedelivery(Integer dmqMaxMsgRedelivery) {
		this.dmqMaxMsgRedelivery = dmqMaxMsgRedelivery;
	}

	public Integer getDmqMaxMsgSize() {
		return dmqMaxMsgSize;
	}

	public void setDmqMaxMsgSize(Integer dmqMaxMsgSize) {
		this.dmqMaxMsgSize = dmqMaxMsgSize;
	}

	public Integer getDmqQuota() {
		return dmqQuota;
	}

	public void setDmqQuota(Integer dmqQuota) {
		this.dmqQuota = dmqQuota;
	}

	public Boolean getDmqRespectsMsgTTL() {
		return dmqRespectsMsgTTL;
	}

	public void setDmqRespectsMsgTTL(Boolean dmqRespectsMsgTTL) {
		this.dmqRespectsMsgTTL = dmqRespectsMsgTTL;
	}
}
