package org.vaadin.tatu.vaadincreate.backend.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.tatu.vaadincreate.backend.RedisPubSubService.EventEnvelope;
import org.vaadin.tatu.vaadincreate.backend.events.MessageEvent;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class RedisPubSubServiceTest {

    private RedisPubSubServiceImpl service;

    @Before
    public void setUp() {
        service = new RedisPubSubServiceImpl("localhost", 6379, "test_channel",
                null);
    }

    @After
    public void tearDown() {
        service.stopSubscriber();
        service.closePublisher();
    }

    @Test
    public void testPublishEventSuccessful() throws Exception {
        // Arrange: Replace the publisherJedis with a mock.
        var publisherMock = mock(Jedis.class);
        service.publisherJedis = publisherMock;

        // Act: Call publishEvent.
        service.publishEvent("node1",
                new MessageEvent("testEvent", LocalDateTime.now()));

        // Assert: Verify publish was called with a proper JSON message.
        verify(publisherMock).publish(eq("test_channel"), anyString());
        // Capture and verify the published message.
        String publishedMessage = getPublishedMessage(publisherMock);
        var mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        var envelope = mapper.readValue(publishedMessage, EventEnvelope.class);
        assertEquals("node1", envelope.nodeId());
        assertEquals("testEvent", ((MessageEvent) envelope.event()).message());
    }

    @Test
    public void testPublishEventSetsLocalModeOnException() {
        // Arrange: Replace the publisherJedis with a mock that throws
        // exception.
        var publisherMock = mock(Jedis.class);
        service.publishEvent("node1",
                new MessageEvent("testEvent", LocalDateTime.now()));

        doThrow(new JedisConnectionException("Test exception"))
                .when(publisherMock).publish(anyString(), anyString());

        // Act: Call publishEvent which should catch exception and set
        // localMode.
        service.publishEvent("node1",
                new MessageEvent("testEvent", LocalDateTime.now()));

        // Reset the mock to verify that subsequent calls do nothing.
        reset(publisherMock);
        service.publishEvent("node1",
                new MessageEvent("testEvent", LocalDateTime.now()));

        // Assert: publish should not be called after localMode is set.
        verify(publisherMock, never()).publish(anyString(), anyString());
    }

    @Test
    public void testStartSubscriberInvokesConsumerOnMessage() throws Exception {
        // Arrange: Replace subscriberJedis with a mock.
        var latch = new CountDownLatch(1);
        var subscriberMock = mock(Jedis.class);
        service.subscriberJedis = subscriberMock;

        @SuppressWarnings("unchecked")
        Consumer<EventEnvelope> envelopeConsumer = mock(Consumer.class);

        // Stub subscriberJedis.subscribe to simulate arrival of a message.
        doAnswer(invocation -> {
            JedisPubSub pubSub = invocation.getArgument(0);
            // Simulate a received message.
            var envelope = new EventEnvelope("node1",
                    new MessageEvent("testEvent", LocalDateTime.now()));
            var mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            var json = mapper.writeValueAsString(envelope);
            pubSub.onMessage("test_channel", json);
            latch.countDown();
            return null;
        }).when(subscriberMock).subscribe(any(JedisPubSub.class),
                eq("test_channel"));

        // Act: Start the subscriber.
        service.startSubscriber(envelopeConsumer);
        // Allow some time for the asynchronous execution.
        latch.await();

        // Assert: Verify that the consumer was invoked.
        verify(envelopeConsumer, atLeastOnce())
                .accept(any(EventEnvelope.class));
    }

    // Helper method to extract the published message from the publisher mock.
    private String getPublishedMessage(Jedis publisherMock) {
        // Verify that publish was called and capture the published message.
        // Since we cannot capture the argument directly, we use an Answer.
        var capturedMessage = new String[1];
        doAnswer(invocation -> {
            capturedMessage[0] = invocation.getArgument(1);
            return 1L;
        }).when(publisherMock).publish(anyString(), anyString());
        // Re-call publishEvent to capture the message.
        service.publishEvent("node1",
                new MessageEvent("testEvent", LocalDateTime.now()));
        return capturedMessage[0];
    }
}
