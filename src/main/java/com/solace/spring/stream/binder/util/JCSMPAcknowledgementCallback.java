package com.solace.spring.stream.binder.util;

import com.solacesystems.jcsmp.XMLMessage;
import org.springframework.integration.support.AcknowledgmentCallback;
import org.springframework.integration.support.AcknowledgmentCallbackFactory;

class JCSMPAcknowledgementCallbackFactory implements AcknowledgmentCallbackFactory<XMLMessage> {

	@Override
	public AcknowledgmentCallback createCallback(XMLMessage xmlMessage) {
		return new JCSMPAcknowledgementCallback(xmlMessage);
	}

	private static class JCSMPAcknowledgementCallback implements AcknowledgmentCallback {
		private XMLMessage xmlMessage;
		private boolean acknowledged = false;
		private boolean autoAckEnabled = true;

		JCSMPAcknowledgementCallback(XMLMessage xmlMessage) {
			this.xmlMessage = xmlMessage;
		}

		@Override
		public void acknowledge(Status status) {
			switch (status) {
				case ACCEPT:
					xmlMessage.ackMessage();
					break;
				case REJECT:
					break; // No ack...
				case REQUEUE:
					throw new UnsupportedOperationException("Requeue acknowledgement is not supported in the Solace binder");
			}

			acknowledged = true;
		}

		@Override
		public boolean isAcknowledged() {
			return acknowledged;
		}

		@Override
		public void noAutoAck() {
			autoAckEnabled = false;
		}

		@Override
		public boolean isAutoAck() {
			return autoAckEnabled;
		}
	}
}
