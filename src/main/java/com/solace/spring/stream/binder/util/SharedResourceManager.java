package com.solace.spring.stream.binder.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Set;

abstract class SharedResourceManager<T> {
	private final String type;
	T sharedResource;
	private Set<String> registeredIds = new HashSet<>();
	private final Object lock = new Object();

	private static final Log logger = LogFactory.getLog(SharedResourceManager.class);

	SharedResourceManager(String type) {
		this.type = type;
	}

	abstract T create() throws Exception;
	abstract void close();

	public T get(String callerID) throws Exception {
		synchronized (lock) {
			if (registeredIds.isEmpty()) {
				logger.info(String.format("No %s exists, a new one will be created", type));
				sharedResource = create();
			} else {
				logger.info(String.format("A message %s already exists, reusing it", type));
			}

			registeredIds.add(callerID);
		}

		return sharedResource;
	}

	public void close(String callerId) {
		synchronized (lock) {
			if (registeredIds.contains(callerId) && registeredIds.size() <= 1) {
				logger.info(String.format("%s is the last user, closing %s...", callerId, type));
				close();
				sharedResource = null;
			} else {
				logger.info(String.format("%s is not the last user, persisting %s...", callerId, type));
			}
			registeredIds.remove(callerId);
		}
	}
}
