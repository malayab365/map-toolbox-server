package com.example.mcptoolbox.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisToolsTest {

    @Mock ObjectProvider<StringRedisTemplate> provider;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    private RedisTools tools;

    @BeforeEach
    void setUp() {
        tools = new RedisTools(provider);
    }

    // --- redis() throws when not configured ---

    @Test
    void redisGet_noRedis_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.redisGet("key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis not configured");
    }

    // --- redisGet ---

    @Test
    void redisGet_existingKey_returnsValue() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("myKey")).thenReturn("myValue");

        assertThat(tools.redisGet("myKey")).isEqualTo("myValue");
    }

    @Test
    void redisGet_missingKey_returnsNull() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("gone")).thenReturn(null);

        assertThat(tools.redisGet("gone")).isNull();
    }

    // --- redisSet ---

    @Test
    void redisSet_withTtl_setsWithExpiry() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);

        String result = tools.redisSet("k", "v", 60L);

        verify(valueOps).set("k", "v", Duration.ofSeconds(60));
        assertThat(result).isEqualTo("OK");
    }

    @Test
    void redisSet_withZeroTtl_setsWithoutExpiry() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);

        String result = tools.redisSet("k", "v", 0L);

        verify(valueOps).set("k", "v");
        assertThat(result).isEqualTo("OK");
    }

    @Test
    void redisSet_withNegativeTtl_setsWithoutExpiry() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);

        tools.redisSet("k", "v", -5L);

        verify(valueOps).set("k", "v");
    }

    @Test
    void redisSet_nullTtl_setsWithoutExpiry() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(valueOps);

        String result = tools.redisSet("k", "v", null);

        verify(valueOps).set("k", "v");
        assertThat(result).isEqualTo("OK");
    }

    @Test
    void redisSet_noRedis_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.redisSet("k", "v", null))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- redisDelete ---

    @Test
    void redisDelete_existingKey_returnsTrue() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.delete("k")).thenReturn(true);

        assertThat(tools.redisDelete("k")).isTrue();
    }

    @Test
    void redisDelete_missingKey_returnsFalse() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.delete("k")).thenReturn(false);

        assertThat(tools.redisDelete("k")).isFalse();
    }

    @Test
    void redisDelete_nullReturn_returnsFalse() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.delete("k")).thenReturn(null);

        assertThat(tools.redisDelete("k")).isFalse();
    }

    @Test
    void redisDelete_noRedis_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.redisDelete("k"))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- redisKeys ---

    @Test
    void redisKeys_returnsMatchingKeys() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.keys("user:*")).thenReturn(Set.of("user:1", "user:2"));

        Set<String> result = tools.redisKeys("user:*");

        assertThat(result).containsExactlyInAnyOrder("user:1", "user:2");
    }

    @Test
    void redisKeys_noRedis_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.redisKeys("*"))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- redisTtl ---

    @Test
    void redisTtl_keyWithExpiry_returnsSeconds() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.getExpire("k")).thenReturn(120L);

        assertThat(tools.redisTtl("k")).isEqualTo(120L);
    }

    @Test
    void redisTtl_persistentKey_returnsMinusOne() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.getExpire("k")).thenReturn(-1L);

        assertThat(tools.redisTtl("k")).isEqualTo(-1L);
    }

    @Test
    void redisTtl_missingKey_returnsMinusTwo() {
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.getExpire("k")).thenReturn(-2L);

        assertThat(tools.redisTtl("k")).isEqualTo(-2L);
    }

    @Test
    void redisTtl_noRedis_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.redisTtl("k"))
                .isInstanceOf(IllegalStateException.class);
    }
}
