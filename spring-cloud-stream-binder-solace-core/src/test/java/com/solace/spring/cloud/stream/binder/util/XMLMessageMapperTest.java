package com.solace.spring.cloud.stream.binder.util;

import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;
import com.solace.spring.cloud.stream.binder.properties.SolaceProducerProperties;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.XMLMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.SerializationUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class XMLMessageMapperTest {
	@Spy
	@InjectMocks
	private XMLMessageMapper xmlMessageMapper = new XMLMessageMapper();

	@Mock
	private XMLMessageMapper.MessageWrapperUtils messageWrapperUtils;

	@Before
	public void setupMockito() {
		MockitoAnnotations.initMocks(this);
		setupMessageWrapperExpectations(null, null, null);
	}

	@Test
	public void testMapSpringMessageToXMLMessage() {
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory().withPayload("testPayload").build();

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage);
		Mockito.verify(messageWrapperUtils).createMessageWrapper(testSpringMessage);

		Object attachment = SerializationUtils.deserialize(xmlMessage.getAttachmentByteBuffer().array());
		Assert.assertNotNull(attachment);
		Assert.assertEquals(XMLMessageMapper.MessageWrapper.class, attachment.getClass());
		Assert.assertEquals(XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT, xmlMessage.getHTTPContentType());
		Assert.assertEquals(DeliveryMode.PERSISTENT, xmlMessage.getDeliveryMode());
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
	public void testMapXMLMessageToSpringMessageByteArray() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.setHTTPContentType(XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT);

		byte[] expectedPayload = "testPayload".getBytes(XMLMessageMapper.DEFAULT_ENCODING);

		HashMap<String,Object> expectedHeaders = new HashMap<>();
		expectedHeaders.put("test-header-1", "test-header-val-1");
		expectedHeaders.put("test-header-2", "test-header-val-2");

		XMLMessageMapper.MessageWrapper wrapper = setupMessageWrapperExpectations(
				expectedHeaders, expectedPayload, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE);
		xmlMessage.writeAttachment(SerializationUtils.serialize(wrapper));

		Message<?> springMessage = xmlMessageMapper.map(xmlMessage);
		Mockito.verify(xmlMessageMapper).map(xmlMessage, false);
		Mockito.verify(messageWrapperUtils).extractMessageWrapper(xmlMessage);

		Assert.assertEquals(byte[].class, springMessage.getPayload().getClass());
		Assert.assertArrayEquals(expectedPayload, (byte[])springMessage.getPayload());
		validateSpringMessage(springMessage, expectedHeaders);
	}

	@Test
	public void testMapXMLMessageToSpringMessageString() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.setHTTPContentType(XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT);

		Charset charset = XMLMessageMapper.DEFAULT_ENCODING;
		String expectedPayload = "testPayload";

		HashMap<String,Object> expectedHeaders = new HashMap<>();
		expectedHeaders.put("test-header-1", "test-header-val-1");
		expectedHeaders.put("test-header-2", "test-header-val-2");

		XMLMessageMapper.MessageWrapper wrapper = setupMessageWrapperExpectations(
				expectedHeaders, expectedPayload.getBytes(charset), MimeTypeUtils.TEXT_PLAIN_VALUE);
		wrapper.setCharset(charset.name());
		xmlMessage.writeAttachment(SerializationUtils.serialize(wrapper));

		Message<?> springMessage = xmlMessageMapper.map(xmlMessage);
		Mockito.verify(xmlMessageMapper).map(xmlMessage, false);
		Mockito.verify(messageWrapperUtils).extractMessageWrapper(xmlMessage);

		Assert.assertEquals(String.class, springMessage.getPayload().getClass());
		Assert.assertEquals(expectedPayload, springMessage.getPayload());
		validateSpringMessage(springMessage, expectedHeaders);
	}

	@Test
	public void testMapXMLMessageToSpringMessageSerializable() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.setHTTPContentType(XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT);

		SerializableFoo expectedPayload = new SerializableFoo("abc123", "HOOPLA!");

		HashMap<String,Object> expectedHeaders = new HashMap<>();
		expectedHeaders.put("test-header-1", "test-header-val-1");
		expectedHeaders.put("test-header-2", "test-header-val-2");

		XMLMessageMapper.MessageWrapper wrapper = setupMessageWrapperExpectations(
				expectedHeaders,
				SerializationUtils.serialize(expectedPayload),
				XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT);

		xmlMessage.writeAttachment(SerializationUtils.serialize(wrapper));

		Message<?> springMessage = xmlMessageMapper.map(xmlMessage);
		Mockito.verify(xmlMessageMapper).map(xmlMessage, false);
		Mockito.verify(messageWrapperUtils).extractMessageWrapper(xmlMessage);

		Assert.assertEquals(SerializableFoo.class, springMessage.getPayload().getClass());
		Assert.assertEquals(expectedPayload, springMessage.getPayload());
		validateSpringMessage(springMessage, expectedHeaders);
	}

	@Test
	public void testMapXMLMessageToSpringMessageWithRawMessageHeader() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.setHTTPContentType(XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT);

		byte[] expectedPayload = "testPayload".getBytes(XMLMessageMapper.DEFAULT_ENCODING);

		HashMap<String,Object> expectedHeaders = new HashMap<>();
		expectedHeaders.put("test-header-1", "test-header-val-1");
		expectedHeaders.put("test-header-2", "test-header-val-2");

		XMLMessageMapper.MessageWrapper wrapper = setupMessageWrapperExpectations(
				expectedHeaders, expectedPayload, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE);
		xmlMessage.writeAttachment(SerializationUtils.serialize(wrapper));

		Message<?> springMessage = xmlMessageMapper.map(xmlMessage, true);
		Mockito.verify(messageWrapperUtils).extractMessageWrapper(xmlMessage);

		Assert.assertEquals(byte[].class, springMessage.getPayload().getClass());
		Assert.assertArrayEquals(expectedPayload, (byte[])springMessage.getPayload());
		validateSpringMessage(springMessage, expectedHeaders);

		Assert.assertEquals(xmlMessage,
				springMessage.getHeaders().get(SolaceMessageHeaderErrorMessageStrategy.SOLACE_RAW_MESSAGE));
	}

	private void validateSpringMessage(Message<?> message, Map<String,Object> expectedHeaders) {
		MessageHeaders messageHeaders = message.getHeaders();

		for (Map.Entry<String,Object> entry : expectedHeaders.entrySet()) {
			Assert.assertEquals(entry.getValue(), messageHeaders.get(entry.getKey()));
		}

		Object ackCallback = messageHeaders.get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK);
		Assert.assertNotNull(ackCallback);
		Assert.assertEquals(JCSMPAcknowledgementCallbackFactory.JCSMPAcknowledgementCallback.class,
				ackCallback.getClass());

		Object deliveryAttempt = messageHeaders.get(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT);
		Assert.assertNotNull(deliveryAttempt);
		Assert.assertEquals(AtomicInteger.class, deliveryAttempt.getClass());
		Assert.assertEquals(0, ((AtomicInteger) deliveryAttempt).get());
	}

	private XMLMessageMapper.MessageWrapper setupMessageWrapperExpectations(Map<String,Object> headers, byte[] payload, String payloadMimeType) {
		XMLMessageMapper.MessageWrapper testWrapper = new XMLMessageMapper.MessageWrapper(
				new MessageHeaders(headers), payload, payloadMimeType);

		Mockito.when(messageWrapperUtils.extractMessageWrapper(Mockito.any(XMLMessage.class))).thenReturn(testWrapper);
		Mockito.when(messageWrapperUtils.createMessageWrapper(Mockito.any(Message.class))).thenReturn(testWrapper);
		return testWrapper;
	}
}
