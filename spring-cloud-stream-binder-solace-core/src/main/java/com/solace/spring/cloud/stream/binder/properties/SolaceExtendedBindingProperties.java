package com.solace.spring.cloud.stream.binder.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("spring.cloud.stream.solace")
public class SolaceExtendedBindingProperties implements ExtendedBindingProperties<SolaceConsumerProperties,SolaceProducerProperties> {

	private Map<String,SolaceBindingProperties> bindings = new HashMap<>();

	@Override
	public synchronized SolaceConsumerProperties getExtendedConsumerProperties(String channelName) {
		if (bindings.containsKey(channelName)) {
			if (bindings.get(channelName).getConsumer() == null) {
				bindings.get(channelName).setConsumer(new SolaceConsumerProperties());
			}
		} else {
			SolaceBindingProperties bindingProperties = new SolaceBindingProperties();
			bindingProperties.setConsumer(new SolaceConsumerProperties());
			bindings.put(channelName, bindingProperties);
		}

		return bindings.get(channelName).getConsumer();
	}

	@Override
	public synchronized SolaceProducerProperties getExtendedProducerProperties(String channelName) {
		if (bindings.containsKey(channelName)) {
			if (bindings.get(channelName).getProducer() == null) {
				bindings.get(channelName).setProducer(new SolaceProducerProperties());
			}
		} else {
			SolaceBindingProperties bindingProperties = new SolaceBindingProperties();
			bindingProperties.setProducer(new SolaceProducerProperties());
			bindings.put(channelName, bindingProperties);
		}

		return bindings.get(channelName).getProducer();
	}

	private static final String DEFAULTS_PREFIX = "spring.cloud.stream.solace.default";

	@Override
	public String getDefaultsPrefix() {
		return DEFAULTS_PREFIX;
	}

	@Override
	public Class <? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
		return SolaceBindingProperties.class;
	}

	public Map<String, SolaceBindingProperties> getBindings() {
		return bindings;
	}

	public void setBindings(Map<String, SolaceBindingProperties> bindings) {
		this.bindings = bindings;
	}
}
