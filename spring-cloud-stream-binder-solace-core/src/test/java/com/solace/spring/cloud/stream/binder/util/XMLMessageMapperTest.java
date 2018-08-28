package com.solace.spring.cloud.stream.binder.util;

import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;
import com.solace.spring.cloud.stream.binder.properties.SolaceProducerProperties;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.XMLMessage;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.SerializationUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class XMLMessageMapperTest {
	@Spy
	private XMLMessageMapper xmlMessageMapper = new XMLMessageMapper();

	@Before
	public void setupMockito() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testMapSpringMessageToXMLMessage() throws Exception {
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload("testPayload")
				.setHeader("test-header-1", "test-header-val-1")
				.setHeader("test-header-2", "test-header-val-2")
				.build();

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage);

		Assert.assertThat(xmlMessage, CoreMatchers.instanceOf(TextMessage.class));
		Assert.assertEquals(testSpringMessage.getPayload(), ((TextMessage) xmlMessage).getText());
		Assert.assertEquals(DeliveryMode.PERSISTENT, xmlMessage.getDeliveryMode());

		SDTMap metadata = xmlMessage.getProperties();

		for (Map.Entry<String,Object> header : testSpringMessage.getHeaders().entrySet()) {
			Assert.assertTrue(metadata.containsKey(header.getKey()));

			Object actualValue = metadata.get(header.getKey());
			if (metadata.containsKey(xmlMessageMapper.getIsHeaderSerializedMetadataKey(header.getKey()))) {
				actualValue = SerializationUtils.deserialize(metadata.getBytes(header.getKey()));
			}
			Assert.assertEquals(header.getValue(), actualValue);
		}
	}

	@Test
	public void testMapProducerSpringMessageToXMLMessage() {
		String testPayload = "testPayload";
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory().withPayload(testPayload).build();

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage, new SolaceProducerProperties());
		Mockito.verify(xmlMessageMapper).map(testSpringMessage);

		Assert.assertFalse(xmlMessage.isDMQEligible());
		Assert.assertEquals(0, xmlMessage.getTimeToLive());
	}

	@Test
	public void testMapProducerSpringMessageToXMLMessageWithProperties() {
		String testPayload = "testPayload";
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory().withPayload(testPayload).build();
		SolaceProducerProperties producerProperties = new SolaceProducerProperties();
		producerProperties.setMsgInternalDmqEligible(true);
		producerProperties.setMsgTtl(100L);

		XMLMessage xmlMessage1 = xmlMessageMapper.map(testSpringMessage, producerProperties);
		Mockito.verify(xmlMessageMapper).map(testSpringMessage);

		Assert.assertTrue(xmlMessage1.isDMQEligible());
		Assert.assertEquals(producerProperties.getMsgTtl().longValue(), xmlMessage1.getTimeToLive());
	}

	@Test
	public void testMapConsumerSpringMessageToXMLMessage() {
		String testPayload = "testPayload";
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory().withPayload(testPayload).build();

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage, new SolaceConsumerProperties());
		Mockito.verify(xmlMessageMapper).map(testSpringMessage);

		Assert.assertEquals(0, xmlMessage.getTimeToLive());
	}

	@Test
	public void testMapConsumerSpringMessageToXMLMessageWithProperties() {
		String testPayload = "testPayload";
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory().withPayload(testPayload).build();
		SolaceConsumerProperties consumerProperties = new SolaceConsumerProperties();
		consumerProperties.setRepublishedMsgTtl(100L);

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage, consumerProperties);
		Mockito.verify(xmlMessageMapper).map(testSpringMessage);

		Assert.assertEquals(consumerProperties.getRepublishedMsgTtl().longValue(), xmlMessage.getTimeToLive());
	}

	@Test
	public void testMapXMLMessageToSpringMessageByteArray() throws Exception {
		BytesMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
		xmlMessage.setData("testPayload".getBytes(StandardCharsets.UTF_8));
		SDTMap metadata = JCSMPFactory.onlyInstance().createMap();
		metadata.putString("test-header-1", "test-header-val-1");
		metadata.putString("test-header-2", "test-header-val-2");
		xmlMessage.setProperties(metadata);

		Message<?> springMessage = xmlMessageMapper.map(xmlMessage);
		Mockito.verify(xmlMessageMapper).map(xmlMessage, false);

		Assert.assertThat(springMessage.getPayload(), CoreMatchers.instanceOf(byte[].class));
		Assert.assertArrayEquals(xmlMessage.getData(), (byte[])springMessage.getPayload());
		validateSpringMessage(springMessage, metadata);
	}

	@Test
	public void testMapXMLMessageToSpringMessageString() throws Exception {
		TextMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
		xmlMessage.setText("testPayload");
		SDTMap metadata = JCSMPFactory.onlyInstance().createMap();
		metadata.putString("test-header-1", "test-header-val-1");
		metadata.putString("test-header-2", "test-header-val-2");
		xmlMessage.setProperties(metadata);

		Message<?> springMessage = xmlMessageMapper.map(xmlMessage);
		Mockito.verify(xmlMessageMapper).map(xmlMessage, false);

		Assert.assertThat(springMessage.getPayload(), CoreMatchers.instanceOf(String.class));
		Assert.assertEquals(xmlMessage.getText(), springMessage.getPayload());
		validateSpringMessage(springMessage, metadata);
	}

	@Test
	public void testMapXMLMessageToSpringMessageSerializable() throws Exception {
		BytesMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
		SerializableFoo expectedPayload = new SerializableFoo("abc123", "HOOPLA!!");
		xmlMessage.setData(SerializationUtils.serialize(expectedPayload));
		SDTMap metadata = JCSMPFactory.onlyInstance().createMap();
		metadata.putString("test-header-1", "test-header-val-1");
		metadata.putString("test-header-2", "test-header-val-2");
		metadata.putBoolean(XMLMessageMapper.JAVA_SERIALIZED_OBJECT_HEADER, true);
		xmlMessage.setProperties(metadata);

		Message<?> springMessage = xmlMessageMapper.map(xmlMessage);
		Mockito.verify(xmlMessageMapper).map(xmlMessage, false);

		Assert.assertThat(springMessage.getPayload(), CoreMatchers.instanceOf(SerializableFoo.class));
		Assert.assertEquals(expectedPayload, springMessage.getPayload());
		validateSpringMessage(springMessage, metadata);
	}

	@Test
	public void testMapXMLMessageToSpringMessageWithRawMessageHeader() throws Exception {
		BytesMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
		SerializableFoo expectedPayload = new SerializableFoo("abc123", "HOOPLA!!");
		xmlMessage.setData(SerializationUtils.serialize(expectedPayload));
		SDTMap metadata = JCSMPFactory.onlyInstance().createMap();
		metadata.putString("test-header-1", "test-header-val-1");
		metadata.putString("test-header-2", "test-header-val-2");
		metadata.putBoolean(XMLMessageMapper.JAVA_SERIALIZED_OBJECT_HEADER, true);
		xmlMessage.setProperties(metadata);

		Message<?> springMessage = xmlMessageMapper.map(xmlMessage, true);

		Assert.assertThat(springMessage.getPayload(), CoreMatchers.instanceOf(SerializableFoo.class));
		Assert.assertEquals(expectedPayload, springMessage.getPayload());
		validateSpringMessage(springMessage, metadata);

		Assert.assertEquals(xmlMessage,
				springMessage.getHeaders().get(SolaceMessageHeaderErrorMessageStrategy.SOLACE_RAW_MESSAGE));
	}

	@Test(expected = SolaceMessageConversionException.class)
	public void testFailMapXMLMessageToSpringMessageWithNullPayload() {
		BytesMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
		xmlMessageMapper.map(xmlMessage);
	}

	private void validateSpringMessage(Message<?> message, SDTMap expectedHeaders) throws SDTException {
		MessageHeaders messageHeaders = message.getHeaders();

		for (String customHeaderName : XMLMessageMapper.CUSTOM_HEADERS) {
			Assert.assertFalse(
					String.format("Unexpected custom header %s was found in the Spring Message", customHeaderName),
					messageHeaders.containsKey(customHeaderName));
		}

		for (String headerName : expectedHeaders.keySet()) {
			if (XMLMessageMapper.CUSTOM_HEADERS.contains(headerName)) continue;
			Assert.assertEquals(expectedHeaders.get(headerName), messageHeaders.get(headerName));
		}

		Object ackCallback = messageHeaders.get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK);
		Assert.assertNotNull(ackCallback);
		Assert.assertThat(ackCallback,
				CoreMatchers.instanceOf(JCSMPAcknowledgementCallbackFactory.JCSMPAcknowledgementCallback.class));

		Object deliveryAttempt = messageHeaders.get(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT);
		Assert.assertNotNull(deliveryAttempt);
		Assert.assertThat(deliveryAttempt, CoreMatchers.instanceOf(AtomicInteger.class));
		Assert.assertEquals(0, ((AtomicInteger) deliveryAttempt).get());
	}
}
