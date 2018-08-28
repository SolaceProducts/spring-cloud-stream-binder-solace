package com.solace.spring.cloud.stream.binder.util;

import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;
import com.solace.spring.cloud.stream.binder.properties.SolaceProducerProperties;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.DeliveryMode;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.SDTStream;
import com.solacesystems.jcsmp.StreamMessage;
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
import java.util.HashMap;
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
	public void testMapSpringMessageToXMLMessage_ByteArray() throws Exception {
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload("testPayload".getBytes(StandardCharsets.UTF_8))
				.setHeader("test-header-1", "test-header-val-1")
				.setHeader("test-header-2", "test-header-val-2")
				.build();

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage);

		Assert.assertThat(xmlMessage, CoreMatchers.instanceOf(BytesMessage.class));
		Assert.assertEquals(testSpringMessage.getPayload(), ((BytesMessage) xmlMessage).getData());
		validateXMLMessage(xmlMessage, testSpringMessage.getHeaders());
	}

	@Test
	public void testMapSpringMessageToXMLMessage_String() throws Exception {
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload("testPayload")
				.setHeader("test-header-1", "test-header-val-1")
				.setHeader("test-header-2", "test-header-val-2")
				.build();

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage);

		Assert.assertThat(xmlMessage, CoreMatchers.instanceOf(TextMessage.class));
		Assert.assertEquals(testSpringMessage.getPayload(), ((TextMessage) xmlMessage).getText());
		validateXMLMessage(xmlMessage, testSpringMessage.getHeaders());
	}

	@Test
	public void testMapSpringMessageToXMLMessage_Serializable() throws Exception {
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload(new SerializableFoo("abc123", "HOOPLA!"))
				.setHeader("test-header-1", "test-header-val-1")
				.setHeader("test-header-2", "test-header-val-2")
				.build();

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage);

		Assert.assertThat(xmlMessage, CoreMatchers.instanceOf(BytesMessage.class));
		Assert.assertEquals(testSpringMessage.getPayload(),
				SerializationUtils.deserialize(((BytesMessage) xmlMessage).getData()));
		Assert.assertThat(xmlMessage.getProperties().keySet(),
				CoreMatchers.hasItem(XMLMessageMapper.JAVA_SERIALIZED_OBJECT_HEADER));
		Assert.assertEquals(true, xmlMessage.getProperties().getBoolean(XMLMessageMapper.JAVA_SERIALIZED_OBJECT_HEADER));
		validateXMLMessage(xmlMessage, testSpringMessage.getHeaders());
	}

	@Test
	public void testMapSpringMessageToXMLMessage_STDStream() throws Exception {
		SDTStream sdtStream = JCSMPFactory.onlyInstance().createStream();
		sdtStream.writeBoolean(true);
		sdtStream.writeCharacter('s');
		sdtStream.writeMap(JCSMPFactory.onlyInstance().createMap());
		sdtStream.writeStream(JCSMPFactory.onlyInstance().createStream());
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload(sdtStream)
				.setHeader("test-header-1", "test-header-val-1")
				.setHeader("test-header-2", "test-header-val-2")
				.build();

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage);

		Assert.assertThat(xmlMessage, CoreMatchers.instanceOf(StreamMessage.class));
		Assert.assertEquals(testSpringMessage.getPayload(), ((StreamMessage) xmlMessage).getStream());
		validateXMLMessage(xmlMessage, testSpringMessage.getHeaders());
	}

	@Test
	public void testMapSpringMessageToXMLMessage_STDMap() throws Exception {
		SDTMap sdtMap = JCSMPFactory.onlyInstance().createMap();
		sdtMap.putBoolean("a", true);
		sdtMap.putCharacter("b", 's');
		sdtMap.putMap("c", JCSMPFactory.onlyInstance().createMap());
		sdtMap.putStream("d", JCSMPFactory.onlyInstance().createStream());
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload(sdtMap)
				.setHeader("test-header-1", "test-header-val-1")
				.setHeader("test-header-2", "test-header-val-2")
				.build();

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage);

		Assert.assertThat(xmlMessage, CoreMatchers.instanceOf(MapMessage.class));
		Assert.assertEquals(testSpringMessage.getPayload(), ((MapMessage) xmlMessage).getMap());
		validateXMLMessage(xmlMessage, testSpringMessage.getHeaders());
	}

	@Test(expected = SolaceMessageConversionException.class)
	public void testFailMapSpringMessageToXMLMessage_InvalidPayload() {
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory().withPayload(new Object()).build();
		xmlMessageMapper.map(testSpringMessage);
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
	public void testMapProducerSpringMessageToXMLMessage_WithProperties() {
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
	public void testMapConsumerSpringMessageToXMLMessage_WithProperties() {
		String testPayload = "testPayload";
		Message<?> testSpringMessage = new DefaultMessageBuilderFactory().withPayload(testPayload).build();
		SolaceConsumerProperties consumerProperties = new SolaceConsumerProperties();
		consumerProperties.setRepublishedMsgTtl(100L);

		XMLMessage xmlMessage = xmlMessageMapper.map(testSpringMessage, consumerProperties);
		Mockito.verify(xmlMessageMapper).map(testSpringMessage);

		Assert.assertEquals(consumerProperties.getRepublishedMsgTtl().longValue(), xmlMessage.getTimeToLive());
	}

	@Test
	public void testMapXMLMessageToSpringMessage_ByteArray() throws Exception {
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
	public void testMapXMLMessageToSpringMessage_String() throws Exception {
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
	public void testMapXMLMessageToSpringMessage_Serializable() throws Exception {
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
	public void testMapXMLMessageToSpringMessage_WithRawMessageHeader() throws Exception {
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
	public void testFailMapXMLMessageToSpringMessage_WithNullPayload() {
		BytesMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
		xmlMessageMapper.map(xmlMessage);
	}

	@Test
	public void testMapMessageHeadersToSDTMap_Serializable() throws Exception {
		String key = "a";
		SerializableFoo value = new SerializableFoo("abc123", "HOOPLA!");
		Map<String,Object> headers = new HashMap<>();
		headers.put(key, value);

		SDTMap sdtMap = xmlMessageMapper.map(new MessageHeaders(headers));

		Assert.assertThat(sdtMap.keySet(), CoreMatchers.hasItem(key));
		Assert.assertThat(sdtMap.keySet(), CoreMatchers.hasItem(xmlMessageMapper.getIsHeaderSerializedMetadataKey(key)));
		Assert.assertEquals(value, SerializationUtils.deserialize(sdtMap.getBytes(key)));
		Assert.assertEquals(true, sdtMap.getBoolean(xmlMessageMapper.getIsHeaderSerializedMetadataKey(key)));
	}

	@Test
	public void testMapSDTMapToMessageHeaders_Serializable() throws Exception {
		String key = "a";
		SerializableFoo value = new SerializableFoo("abc123", "HOOPLA!");
		SDTMap sdtMap = JCSMPFactory.onlyInstance().createMap();
		sdtMap.putObject(key, SerializationUtils.serialize(value));
		sdtMap.putBoolean(xmlMessageMapper.getIsHeaderSerializedMetadataKey(key), true);

		MessageHeaders messageHeaders = xmlMessageMapper.map(sdtMap);

		Assert.assertThat(messageHeaders.keySet(), CoreMatchers.hasItem(key));
		Assert.assertThat(messageHeaders.keySet(),
				CoreMatchers.not(CoreMatchers.hasItem(xmlMessageMapper.getIsHeaderSerializedMetadataKey(key))));
		Assert.assertEquals(value, messageHeaders.get(key));
		Assert.assertNull(messageHeaders.get(xmlMessageMapper.getIsHeaderSerializedMetadataKey(key)));
	}

	private void validateXMLMessage(XMLMessage xmlMessage, MessageHeaders expectedHeaders) throws Exception {
		Assert.assertEquals(DeliveryMode.PERSISTENT, xmlMessage.getDeliveryMode());

		SDTMap metadata = xmlMessage.getProperties();

		for (Map.Entry<String,Object> header : expectedHeaders.entrySet()) {
			Assert.assertTrue(metadata.containsKey(header.getKey()));

			Object actualValue = metadata.get(header.getKey());
			if (metadata.containsKey(xmlMessageMapper.getIsHeaderSerializedMetadataKey(header.getKey()))) {
				actualValue = SerializationUtils.deserialize(metadata.getBytes(header.getKey()));
			}
			Assert.assertEquals(header.getValue(), actualValue);
		}
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
