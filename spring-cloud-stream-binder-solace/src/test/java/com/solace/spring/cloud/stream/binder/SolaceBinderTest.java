package com.solace.spring.cloud.stream.binder;

import com.solace.spring.boot.autoconfigure.SolaceJavaAutoConfiguration;
import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;
import com.solace.spring.cloud.stream.binder.properties.SolaceProducerProperties;
import com.solacesystems.jcsmp.ClosedFacilityException;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.PropertyMismatchException;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SpringJCSMPFactory;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.PartitionCapableBinderTests;
import org.springframework.cloud.stream.binder.Spy;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeTypeUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SolaceJavaAutoConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class SolaceBinderTest
		extends PartitionCapableBinderTests<SolaceTestBinder, ExtendedConsumerProperties<SolaceConsumerProperties>, ExtendedProducerProperties<SolaceProducerProperties>> {

	@Autowired
	private SpringJCSMPFactory springJCSMPFactory;

	@Value("${test.failOnConnectionException:false}")
	private Boolean failOnConnectError;

	private JCSMPSession jcsmpSession;

	private static SolaceExternalResourceHandler externalResource = new SolaceExternalResourceHandler();


	@Override
	protected boolean usesExplicitRouting() {
		return true;
	}

	@Override
	protected String getClassUnderTestName() {
		return this.getClass().getSimpleName();
	}

	@Override
	protected SolaceTestBinder getBinder() throws Exception {
		if (testBinder == null) {
			jcsmpSession = externalResource.assumeAndGetActiveSession(springJCSMPFactory, failOnConnectError);
			testBinder = new SolaceTestBinder(jcsmpSession);
		}
		return testBinder;
	}

	@Override
	protected ExtendedConsumerProperties<SolaceConsumerProperties> createConsumerProperties() {
		return new ExtendedConsumerProperties<>(new SolaceConsumerProperties());
	}

	@Override
	protected ExtendedProducerProperties<SolaceProducerProperties> createProducerProperties() {
		return new ExtendedProducerProperties<>(new SolaceProducerProperties());
	}

	@Override
	public Spy spyOn(String name) {
		return null;
	}

	// NOT YET SUPPORTED ---------------------------------
	@Override
	public void testPartitionedModuleJava() {
		Assume.assumeTrue("Partitioning not currently supported", false);
	}

	@Override
	public void testPartitionedModuleSpEL() {
		Assume.assumeTrue("Partitioning not currently supported", false);
	}
	// ---------------------------------------------------

	@Test
	public void testSendAndReceiveBad() throws Exception {
		SolaceTestBinder binder = getBinder();

		DirectChannel moduleOutputChannel = createBindableChannel("output", new BindingProperties());
		DirectChannel moduleInputChannel = createBindableChannel("input", new BindingProperties());

		String destination0 = String.format("foo%s0", getDestinationNameDelimiter());

		Binding<MessageChannel> producerBinding = binder.bindProducer(
				destination0, moduleOutputChannel, createProducerProperties());
		Binding<MessageChannel> consumerBinding = binder.bindConsumer(
				destination0, "testSendAndReceiveBad", moduleInputChannel, createConsumerProperties());

		Message<?> message = MessageBuilder.withPayload("foo".getBytes())
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
				.build();

		binderBindUnbindLatency();

		final CountDownLatch latch = new CountDownLatch(3);
		moduleInputChannel.subscribe(message1 -> {
			latch.countDown();
			throw new RuntimeException("bad");
		});

		moduleOutputChannel.send(message);
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		producerBinding.unbind();
		consumerBinding.unbind();
	}

	@Test
	public void testProducerErrorChannel() throws Exception {
		SolaceTestBinder binder = getBinder();

		DirectChannel moduleOutputChannel = createBindableChannel("output", new BindingProperties());

		String destination0 = String.format("foo%s0", getDestinationNameDelimiter());
		String destination0EC = String.format("%s%serrors", destination0, getDestinationNameDelimiter());

		ExtendedProducerProperties<SolaceProducerProperties> producerProps = createProducerProperties();
		producerProps.setErrorChannelEnabled(true);
		Binding<MessageChannel> producerBinding = binder.bindProducer(destination0, moduleOutputChannel, producerProps);

		Message<?> message = MessageBuilder.withPayload("foo".getBytes())
				.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
				.build();

		final CountDownLatch latch = new CountDownLatch(2);

		final AtomicReference<Message<?>> errorMessage = new AtomicReference<>();
		binder.getApplicationContext()
				.getBean(destination0EC, SubscribableChannel.class)
				.subscribe(message1 -> {
					errorMessage.set(message1);
					latch.countDown();
				});

		binder.getApplicationContext()
				.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, SubscribableChannel.class)
				.subscribe(message12 -> latch.countDown());

		jcsmpSession.closeSession();

		try {
			moduleOutputChannel.send(message);
			fail("Expected the producer to fail to send the message...");
		} catch (Exception e) {
			logger.info("Successfully threw an exception during message publishing!");
		}

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(errorMessage.get()).isInstanceOf(ErrorMessage.class);
		assertThat(errorMessage.get().getPayload()).isInstanceOf(MessagingException.class);
		assertThat((MessagingException) errorMessage.get().getPayload()).hasCauseInstanceOf(ClosedFacilityException.class);
		producerBinding.unbind();
	}

	@Test
	public void testFailProducerProvisioningOnRequiredQueuePropertyChange() throws Exception {
		SolaceTestBinder binder = getBinder();

		String destination0 = String.format("foo%s0", getDestinationNameDelimiter());
		String group0 = "testFailProducerProvisioningOnRequiredQueuePropertyChange";

		int defaultAccessType = createConsumerProperties().getExtension().getQueueAccessType();
		EndpointProperties endpointProperties = new EndpointProperties();
		endpointProperties.setAccessType((defaultAccessType + 1) % 2);
		Queue queue = JCSMPFactory.onlyInstance().createQueue(destination0 + getDestinationNameDelimiter() + group0);

		logger.info(String.format("Provisioning queue %s with AccessType %s to conflict with defaultAccessType %s",
				queue.getName(), endpointProperties.getAccessType(), defaultAccessType));
		jcsmpSession.provision(queue, endpointProperties, JCSMPSession.WAIT_FOR_CONFIRM);

		DirectChannel moduleOutputChannel = createBindableChannel("output", new BindingProperties());
		Binding<MessageChannel> producerBinding;
		try {
			ExtendedProducerProperties<SolaceProducerProperties> bindingProducerProperties = createProducerProperties();
			bindingProducerProperties.setRequiredGroups(group0);
			producerBinding = binder.bindProducer(destination0, moduleOutputChannel, bindingProducerProperties);
			producerBinding.unbind();
			fail("Expected producer provisioning to fail due to queue property change");
		} catch (ProvisioningException e) {
			assertThat(e).hasCauseInstanceOf(PropertyMismatchException.class);
			logger.info(String.format("Successfully threw a %s exception with cause %s",
					ProvisioningException.class.getSimpleName(), PropertyMismatchException.class.getSimpleName()));
		} finally {
			jcsmpSession.deprovision(queue, JCSMPSession.FLAG_IGNORE_DOES_NOT_EXIST);
		}
	}

	@Test
	public void testFailConsumerProvisioningOnQueuePropertyChange() throws Exception {
		SolaceTestBinder binder = getBinder();

		String destination0 = String.format("foo%s0", getDestinationNameDelimiter());
		String group0 = "testFailConsumerProvisioningOnQueuePropertyChange";

		int defaultAccessType = createConsumerProperties().getExtension().getQueueAccessType();
		EndpointProperties endpointProperties = new EndpointProperties();
		endpointProperties.setAccessType((defaultAccessType + 1) % 2);
		Queue queue = JCSMPFactory.onlyInstance().createQueue(destination0 + getDestinationNameDelimiter() + group0);

		logger.info(String.format("Provisioning queue %s with AccessType %s to conflict with defaultAccessType %s",
				queue.getName(), endpointProperties.getAccessType(), defaultAccessType));
		jcsmpSession.provision(queue, endpointProperties, JCSMPSession.WAIT_FOR_CONFIRM);

		DirectChannel moduleInputChannel = createBindableChannel("input", new BindingProperties());
		Binding<MessageChannel> consumerBinding;
		try {
			consumerBinding = binder.bindConsumer(
					destination0, group0, moduleInputChannel, createConsumerProperties());
			consumerBinding.unbind();
			fail("Expected consumer provisioning to fail due to queue property change");
		} catch (ProvisioningException e) {
			assertThat(e).hasCauseInstanceOf(PropertyMismatchException.class);
			logger.info(String.format("Successfully threw a %s exception with cause %s",
					ProvisioningException.class.getSimpleName(), PropertyMismatchException.class.getSimpleName()));
		} finally {
			jcsmpSession.deprovision(queue, JCSMPSession.FLAG_IGNORE_DOES_NOT_EXIST);
		}
	}

	@Test
	public void testFailConsumerProvisioningOnDmqPropertyChange() throws Exception {
		SolaceTestBinder binder = getBinder();

		String destination0 = String.format("foo%s0", getDestinationNameDelimiter());
		String group0 = "testFailConsumerProvisioningOnDmqPropertyChange";

		int defaultAccessType = createConsumerProperties().getExtension().getQueueAccessType();
		EndpointProperties endpointProperties = new EndpointProperties();
		endpointProperties.setAccessType((defaultAccessType + 1) % 2);
		String dmqName = destination0 + getDestinationNameDelimiter() + group0 + getDestinationNameDelimiter() + "dmq";
		Queue dmq = JCSMPFactory.onlyInstance().createQueue(dmqName);

		logger.info(String.format("Provisioning DMQ %s with AccessType %s to conflict with defaultAccessType %s",
				dmq.getName(), endpointProperties.getAccessType(), defaultAccessType));
		jcsmpSession.provision(dmq, endpointProperties, JCSMPSession.WAIT_FOR_CONFIRM);

		DirectChannel moduleInputChannel = createBindableChannel("input", new BindingProperties());
		Binding<MessageChannel> consumerBinding;
		try {
			ExtendedConsumerProperties<SolaceConsumerProperties> consumerProperties = createConsumerProperties();
			consumerProperties.getExtension().setAutoBindDmq(true);
			consumerBinding = binder.bindConsumer(
					destination0, group0, moduleInputChannel, consumerProperties);
			consumerBinding.unbind();
			fail("Expected consumer provisioning to fail due to DMQ property change");
		} catch (ProvisioningException e) {
			assertThat(e).hasCauseInstanceOf(PropertyMismatchException.class);
			logger.info(String.format("Successfully threw a %s exception with cause %s",
					ProvisioningException.class.getSimpleName(), PropertyMismatchException.class.getSimpleName()));
		} finally {
			jcsmpSession.deprovision(dmq, JCSMPSession.FLAG_IGNORE_DOES_NOT_EXIST);
		}
	}
}
