package com.example.mcptoolbox.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * MCP tools for Redis, backed by {@link StringRedisTemplate} (Lettuce).
 *
 * <p>Connection configured under {@code spring.data.redis.*}.
 */
@Component
public class RedisTools {

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    public RedisTools(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    private StringRedisTemplate redis() {
        StringRedisTemplate t = redisProvider.getIfAvailable();
        if (t == null) {
            throw new IllegalStateException(
                    "Redis not configured. Set spring.data.redis.* in application.yml.");
        }
        return t;
    }

    @Tool(description = "Get the string value stored at a Redis key. Returns null if the key does not exist.")
    public String redisGet(@ToolParam(description = "Redis key.") String key) {
        return redis().opsForValue().get(key);
    }

    @Tool(description = "Set a string value at a Redis key, with an optional TTL in seconds.")
    public String redisSet(
            @ToolParam(description = "Redis key.") String key,
            @ToolParam(description = "Value to store.") String value,
            @ToolParam(required = false, description = "Time-to-live in seconds. Omit for no expiry.") Long ttlSeconds) {
        if (ttlSeconds != null && ttlSeconds > 0) {
            redis().opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            redis().opsForValue().set(key, value);
        }
        return "OK";
    }

    @Tool(description = "Delete a Redis key. Returns true if a key was removed.")
    public boolean redisDelete(@ToolParam(description = "Redis key.") String key) {
        return Boolean.TRUE.equals(redis().delete(key));
    }

    @Tool(description = "Find Redis keys matching a glob-style pattern (e.g. user:*). Use cautiously on large datasets.")
    public Set<String> redisKeys(
            @ToolParam(description = "Match pattern, e.g. 'session:*' or '*'.") String pattern) {
        return redis().keys(pattern);
    }

    @Tool(description = "Get the remaining time-to-live of a Redis key in seconds. -1 = no expiry, -2 = key missing.")
    public Long redisTtl(@ToolParam(description = "Redis key.") String key) {
        return redis().getExpire(key);
    }
}
