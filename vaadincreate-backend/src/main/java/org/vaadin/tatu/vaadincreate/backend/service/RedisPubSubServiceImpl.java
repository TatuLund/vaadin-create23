package org.vaadin.tatu.vaadincreate.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.tatu.vaadincreate.backend.RedisPubSubService;

@NullMarked
public class RedisPubSubServiceImpl implements RedisPubSubService {

    private final Jedis publisherJedis;
    private final Jedis subscriberJedis;
    private final String channel;
    private final ExecutorService executor;
    private final ObjectMapper mapper;
    private boolean localMode = false;

    private static RedisPubSubServiceImpl instance;

    public static synchronized RedisPubSubService getInstance() {
        if (instance == null) {
            instance = new RedisPubSubServiceImpl("redis", 6379,
                    "eventbus_channel");
        }
        return instance;
    }

    protected RedisPubSubServiceImpl(String host, int port, String channel) {
        this.channel = channel;
        this.publisherJedis = new Jedis(host, port);
        this.subscriberJedis = new Jedis(host, port);
        this.executor = Executors.newSingleThreadExecutor();

        // Configure ObjectMapper with support for Java records and polymorphic
        // types.
        this.mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
    }

    @Override
    public void publishEvent(String nodeId, Object event) {
        if (localMode) {
            logger.trace("Local mode is enabled; event not published: {}",
                    event);
            return;
        }
        try {
            var envelope = new EventEnvelope(nodeId, event);
            var message = mapper.writeValueAsString(envelope);
            publisherJedis.publish(channel, message);
            logger.debug("Published event: {}", message);
        } catch (JedisConnectionException e) {
            logger.warn(
                    "Redis is unavailable; falling back to local mode. Error: {}",
                    e.getMessage());
            localMode = true;
        } catch (Exception e) {
            logger.error("Error serializing/publishing event", e);
        }
    }

    @Override
    public void startSubscriber(Consumer<EventEnvelope> envelopeHandler) {
        executor.submit(() -> {
            try {
                subscriberJedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        try {
                            var envelope = mapper.readValue(message,
                                    EventEnvelope.class);
                            envelopeHandler.accept(envelope);
                        } catch (Exception e) {
                            logger.error("Error deserializing event message",
                                    e);
                        }
                    }
                }, channel);
            } catch (JedisConnectionException e) {
                logger.warn(
                        "Redis is unavailable for subscription; running in local-only mode. Error: {}",
                        e.getMessage());
                localMode = true;
            } catch (Exception e) {
                logger.error("Error in Redis subscription", e);
            }
        });
    }

    public void stopSubscriber() {
        if (subscriberJedis != null && subscriberJedis.isConnected()) {
            subscriberJedis.close();
        }
        executor.shutdownNow();
    }

    public void closePublisher() {
        if (publisherJedis != null && publisherJedis.isConnected()) {
            publisherJedis.close();
        }
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

}