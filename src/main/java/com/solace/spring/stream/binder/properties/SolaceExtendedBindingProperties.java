package com.solace.spring.stream.binder.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("spring.cloud.stream.solace") //TODO Are we allowed to put this here?
public class SolaceExtendedBindingProperties implements ExtendedBindingProperties<SolaceConsumerProperties,SolaceProducerProperties> {

	private Map<String,SolaceBindingProperties> channelBindingProperties = new HashMap<>();

	@Override
	public synchronized SolaceConsumerProperties getExtendedConsumerProperties(String channelName) {
		if (channelBindingProperties.containsKey(channelName)) {
			if (channelBindingProperties.get(channelName).getConsumerProperties() == null) {
				channelBindingProperties.get(channelName).setConsumerProperties(new SolaceConsumerProperties());
			}
		} else {
			SolaceBindingProperties bindingProperties = new SolaceBindingProperties();
			bindingProperties.setConsumerProperties(new SolaceConsumerProperties());
			channelBindingProperties.put(channelName, bindingProperties);
		}

		return channelBindingProperties.get(channelName).getConsumerProperties();
	}

	@Override
	public synchronized SolaceProducerProperties getExtendedProducerProperties(String channelName) {
		if (channelBindingProperties.containsKey(channelName)) {
			if (channelBindingProperties.get(channelName).getProducerProperties() == null) {
				channelBindingProperties.get(channelName).setProducerProperties(new SolaceProducerProperties());
			}
		} else {
			SolaceBindingProperties bindingProperties = new SolaceBindingProperties();
			bindingProperties.setProducerProperties(new SolaceProducerProperties());
			channelBindingProperties.put(channelName, bindingProperties);
		}

		return channelBindingProperties.get(channelName).getProducerProperties();
	}

	public Map<String, SolaceBindingProperties> getChannelBindingProperties() {
		return channelBindingProperties;
	}

	public void setChannelBindingProperties(Map<String, SolaceBindingProperties> channelBindingProperties) {
		this.channelBindingProperties = channelBindingProperties;
	}
}
