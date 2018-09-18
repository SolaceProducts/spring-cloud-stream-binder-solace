package com.solace.spring.cloud.stream.binder.inbound;

import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPErrorResponseException;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFlowTransportUnsolicitedUnbindException;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.impl.JCSMPErrorResponseSubcodeMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FlowConnectionSchedulerTest {
	private final JCSMPException[] validExceptions = new JCSMPException[] {
			new JCSMPErrorResponseException(503,
					JCSMPErrorResponseSubcodeMapper.ESTR.ER_QUEUE_SHUTDOWN.getS(), "", "",
					JCSMPErrorResponseSubcodeMapper.ErrorContext.DATA),
			new JCSMPErrorResponseException(503,
					JCSMPErrorResponseSubcodeMapper.ESTR.ER_SERVICE_UNAVAILABLE.getS(), "", "",
					JCSMPErrorResponseSubcodeMapper.ErrorContext.CONTROL),
			new JCSMPFlowTransportUnsolicitedUnbindException("")
	};

	private static final Log logger = LogFactory.getLog(FlowConnectionSchedulerTest.class);

	@Mock
	private JCSMPSession jcsmpSession;

	@Mock
	private FlowReceiver flowReceiver;

	private long retryWaitTime = 500;
	private FlowConnectionScheduler scheduler;

	@Before
	public void setupMockito() throws JCSMPException {
		MockitoAnnotations.initMocks(this);
		scheduler = new FlowConnectionScheduler("test-queue-0", jcsmpSession, new EndpointProperties(), null, retryWaitTime);
		Mockito.when(
				jcsmpSession.createFlow(
						Mockito.any(XMLMessageListener.class),
						Mockito.any(ConsumerFlowProperties.class),
						Mockito.any(EndpointProperties.class)))
				.thenReturn(flowReceiver);

		Mockito.when(jcsmpSession.getSessionName()).thenReturn(UUID.randomUUID().toString());

		Mockito.doNothing().when(flowReceiver).start();
	}

	@After
	public void cleanup() {
		scheduler.shutdown();
	}

	@Test
	public void testCreateFutureFlow() throws Exception {
		Future<FlowReceiver> flowReceiverFuture = scheduler.createFutureFlow();

		Assert.assertEquals(flowReceiver, flowReceiverFuture.get(100, TimeUnit.MILLISECONDS));
		Assert.assertThat(flowReceiverFuture.isDone(), CoreMatchers.is(true));

		Mockito.verify(jcsmpSession)
				.createFlow(Mockito.any(XMLMessageListener.class),
						Mockito.any(ConsumerFlowProperties.class),
						Mockito.any(EndpointProperties.class));

		logger.info(String.format("Successfully got the %s", FlowReceiver.class.getSimpleName()));
	}

	@Test
	public void testCreateFutureFlow_onSuccess() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		Consumer<Queue> onSuccess = (q) -> {
			logger.info("Successfully called the onSuccess Consumer");
			latch.countDown();
		};

		scheduler = new FlowConnectionScheduler("test-queue-0", jcsmpSession, new EndpointProperties(), onSuccess, retryWaitTime);

		Future<FlowReceiver> flowReceiverFuture = scheduler.createFutureFlow();

		Assert.assertThat(latch.await(1000, TimeUnit.MILLISECONDS), CoreMatchers.is(true));
		Assert.assertEquals(flowReceiver, flowReceiverFuture.get(0, TimeUnit.MILLISECONDS));
		Assert.assertThat(flowReceiverFuture.isDone(), CoreMatchers.is(true));

		Mockito.verify(jcsmpSession)
				.createFlow(Mockito.any(XMLMessageListener.class),
						Mockito.any(ConsumerFlowProperties.class),
						Mockito.any(EndpointProperties.class));

		logger.info(String.format("Successfully got the %s", FlowReceiver.class.getSimpleName()));
	}

	@Test
	public void testCreateFutureFlow_queueDown() throws Exception {
		int numErrors = validExceptions.length + 1;
		Stubber stubber = Mockito.doThrow(validExceptions[0]);
		for (JCSMPException validException : validExceptions) {
			stubber = stubber.doThrow(validException);
		}
		stubber.doNothing().when(flowReceiver).start();

		Future<FlowReceiver> flowReceiverFuture = scheduler.createFutureFlow();

		Assert.assertThat(flowReceiverFuture.isDone(), CoreMatchers.is(false));
		long timeout = (retryWaitTime * numErrors) + 100; // +100 to prevent potential race condition
		Assert.assertEquals(flowReceiver, flowReceiverFuture.get(timeout, TimeUnit.MILLISECONDS));
		Assert.assertThat(flowReceiverFuture.isDone(), CoreMatchers.is(true));

		Mockito.verify(jcsmpSession, Mockito.times(numErrors+1))
				.createFlow(Mockito.any(XMLMessageListener.class),
						Mockito.any(ConsumerFlowProperties.class),
						Mockito.any(EndpointProperties.class));

		Mockito.verify(flowReceiver, Mockito.times(numErrors+1)).start();
		logger.info(String.format("Successfully got the %s after %s expected failures",
				FlowReceiver.class.getSimpleName(), numErrors));
	}

	@Test(expected = ExecutionException.class)
	public void testCreateFutureFlow_exception() throws Exception {
		Mockito.doThrow(new JCSMPErrorResponseException(
						400,
						JCSMPErrorResponseSubcodeMapper.ESTR.ER_ENDPOINT_MODIFIED.getS(),
						"",
						"",
						JCSMPErrorResponseSubcodeMapper.ErrorContext.DATA)).when(flowReceiver)
				.start();

		Future<FlowReceiver> flowReceiverFuture = scheduler.createFutureFlow();

		flowReceiverFuture.get(100, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testIsShutdownException() {
		for (JCSMPException e : validExceptions) {
			Assert.assertThat(scheduler.isShutdownException(e), CoreMatchers.is(true));
		}
		Assert.assertThat(scheduler.isShutdownException(new JCSMPException("")), CoreMatchers.is(false));
	}
}
