package com.solace.spring.cloud.stream.binder.util;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.XMLMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.SerializationUtils;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageWrapperUtilsTest {
	private XMLMessageMapper.MessageWrapperUtils messageWrapperUtils = new XMLMessageMapper.MessageWrapperUtils();

	@Test
	public void testCreateMessageWrapperByteArray() {
		byte[] expectedPayload = "testPayload".getBytes(XMLMessageMapper.DEFAULT_ENCODING);
		AtomicInteger expectedDeliveryAttemptHeader = new AtomicInteger(0);
		Message<?> expectedSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload(expectedPayload)
				.setHeader(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, expectedDeliveryAttemptHeader)
				.build();

		XMLMessageMapper.MessageWrapper wrapper = messageWrapperUtils.createMessageWrapper(expectedSpringMessage);

		Assert.assertArrayEquals(expectedPayload, wrapper.getPayload());
		validateMessageWrapper(wrapper, MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE, expectedDeliveryAttemptHeader, null);
	}

	@Test
	public void testCreateMessageWrapperString() {
		Charset expectedCharset = XMLMessageMapper.DEFAULT_ENCODING;
		String expectedPayload = "testPayload";
		AtomicInteger expectedDeliveryAttemptHeader = new AtomicInteger(0);
		Message<?> expectedSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload(expectedPayload)
				.setHeader(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, expectedDeliveryAttemptHeader)
				.build();

		XMLMessageMapper.MessageWrapper wrapper = messageWrapperUtils.createMessageWrapper(expectedSpringMessage);

		Assert.assertEquals(expectedPayload, new String(wrapper.getPayload(), expectedCharset));
		validateMessageWrapper(wrapper, MimeTypeUtils.TEXT_PLAIN_VALUE, expectedDeliveryAttemptHeader, expectedCharset.name());
	}

	@Test
	public void testCreateMessageWrapperSerializable() {
		SerializableFoo expectedPayload = new SerializableFoo("abc123", "hoopla!");
		AtomicInteger expectedDeliveryAttemptHeader = new AtomicInteger(0);
		Message<?> expectedSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload(expectedPayload)
				.setHeader(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, expectedDeliveryAttemptHeader)
				.build();

		XMLMessageMapper.MessageWrapper wrapper = messageWrapperUtils.createMessageWrapper(expectedSpringMessage);

		Assert.assertEquals(expectedPayload, SerializationUtils.deserialize(wrapper.getPayload()));
		validateMessageWrapper(wrapper, XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT, expectedDeliveryAttemptHeader, null);
	}

	@Test(expected = SolaceMessageConversionException.class)
	public void testFailCreateMessageWrapper() {
		Object expectedPayload = new Object();
		AtomicInteger expectedDeliveryAttemptHeader = new AtomicInteger(0);
		Message<?> expectedSpringMessage = new DefaultMessageBuilderFactory()
				.withPayload(expectedPayload)
				.setHeader(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, expectedDeliveryAttemptHeader)
				.build();

		messageWrapperUtils.createMessageWrapper(expectedSpringMessage);
	}

	@Test
	public void testExtractMessageWrapper() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.setHTTPContentType(XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT);
		XMLMessageMapper.MessageWrapper testWrapper = new XMLMessageMapper.MessageWrapper(null, null, null);
		xmlMessage.writeAttachment(SerializationUtils.serialize(testWrapper));
		messageWrapperUtils.extractMessageWrapper(xmlMessage);
	}

	@Test(expected = SolaceMessageConversionException.class)
	public void testFailExtractMessageWrapperNullContentType() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		XMLMessageMapper.MessageWrapper testWrapper = new XMLMessageMapper.MessageWrapper(null, null, null);
		xmlMessage.writeAttachment(SerializationUtils.serialize(testWrapper));
		messageWrapperUtils.extractMessageWrapper(xmlMessage);
	}

	@Test(expected = SolaceMessageConversionException.class)
	public void testFailExtractMessageWrapperInvalidContentType() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.setHTTPContentType("blabla");
		XMLMessageMapper.MessageWrapper testWrapper = new XMLMessageMapper.MessageWrapper(null, null, null);
		xmlMessage.writeAttachment(SerializationUtils.serialize(testWrapper));
		messageWrapperUtils.extractMessageWrapper(xmlMessage);
	}

	@Test(expected = SolaceMessageConversionException.class)
	public void testFailExtractMessageWrapperNullAttachment() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.setHTTPContentType(XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT);
		messageWrapperUtils.extractMessageWrapper(xmlMessage);
	}

	@Test(expected = SolaceMessageConversionException.class)
	public void testFailExtractMessageWrapperEmptyAttachment() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.setHTTPContentType(XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT);
		xmlMessage.writeAttachment(new byte[0]);
		messageWrapperUtils.extractMessageWrapper(xmlMessage);
	}

	@Test(expected = SolaceMessageConversionException.class)
	public void testFailExtractMessageWrapperInvalidAttachment() {
		XMLMessage xmlMessage = JCSMPFactory.onlyInstance().createMessage(BytesXMLMessage.class);
		xmlMessage.setHTTPContentType(XMLMessageMapper.MIME_JAVA_SERIALIZED_OBJECT);
		xmlMessage.writeAttachment(SerializationUtils.serialize(new SerializableFoo("abc123", "HOOPLA!")));
		messageWrapperUtils.extractMessageWrapper(xmlMessage);
	}

	private void validateMessageWrapper(XMLMessageMapper.MessageWrapper wrapper,
										String payloadMimeType, AtomicInteger deliveryAttemptHeader, String charset) {
		Assert.assertEquals(payloadMimeType, wrapper.getPayloadMimeType());
		Object testHeader = wrapper.getHeaders().get(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT);
		Assert.assertNotNull(testHeader);
		Assert.assertEquals(AtomicInteger.class, testHeader.getClass());
		Assert.assertEquals(deliveryAttemptHeader.get(), ((AtomicInteger) testHeader).get());
		Assert.assertEquals(charset, wrapper.getCharset());
	}
}
