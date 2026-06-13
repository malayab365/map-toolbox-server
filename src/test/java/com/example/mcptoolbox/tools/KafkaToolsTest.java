package com.example.mcptoolbox.tools;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaToolsTest {

    @Mock ObjectProvider<KafkaTemplate<String, String>> templateProvider;
    @Mock ObjectProvider<ConsumerFactory<String, String>> consumerFactoryProvider;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock ConsumerFactory<String, String> consumerFactory;
    @Mock Consumer<String, String> consumer;

    private KafkaTools tools;

    @BeforeEach
    void setUp() {
        tools = new KafkaTools(templateProvider, consumerFactoryProvider);
    }

    // --- kafkaPublish ---

    @Test
    void kafkaPublish_noTemplate_throwsIllegalState() {
        when(templateProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.kafkaPublish("topic", "msg", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kafka not configured");
    }

    @Test
    void kafkaPublish_withoutKey_sendsAndReturnsMetadata() throws Exception {
        when(templateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("t", 0), 5L, 0, 0, 0, 0);
        SendResult<String, String> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send("t", "hello")).thenReturn(future);

        String result = tools.kafkaPublish("t", "hello", null);

        assertThat(result).contains("partition=0").contains("offset=5");
    }

    @Test
    void kafkaPublish_withBlankKey_treatedAsNoKey() throws Exception {
        when(templateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("t", 0), 1L, 0, 0, 0, 0);
        SendResult<String, String> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send("t", "hello")).thenReturn(future);

        String result = tools.kafkaPublish("t", "hello", "  ");

        verify(kafkaTemplate).send("t", "hello");
        assertThat(result).contains("t");
    }

    @Test
    void kafkaPublish_withKey_sendsWithKey() throws Exception {
        when(templateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("t", 1), 10L, 0, 0, 0, 0);
        SendResult<String, String> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send("t", "myKey", "hello")).thenReturn(future);

        String result = tools.kafkaPublish("t", "hello", "myKey");

        verify(kafkaTemplate).send("t", "myKey", "hello");
        assertThat(result).contains("partition=1").contains("offset=10");
    }

    @Test
    void kafkaPublish_futureThrows_wrapsException() {
        when(templateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send("t", "msg")).thenReturn(failedFuture);

        assertThatThrownBy(() -> tools.kafkaPublish("t", "msg", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish to Kafka");
    }

    // --- kafkaConsume ---

    @Test
    void kafkaConsume_noFactory_throwsIllegalState() {
        when(consumerFactoryProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.kafkaConsume("topic", null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Kafka not configured");
    }

    @Test
    void kafkaConsume_returnsRecordsUpToMax() {
        when(consumerFactoryProvider.getIfAvailable()).thenReturn(consumerFactory);
        when(consumerFactory.createConsumer(anyString(), anyString())).thenReturn(consumer);

        TopicPartition tp = new TopicPartition("topic", 0);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0, "k", "v");
        ConsumerRecords<String, String> batch = new ConsumerRecords<>(
                Map.of(tp, List.of(record)));

        // Return one batch then empty so loop exits
        when(consumer.poll(any(Duration.class))).thenReturn(batch, new ConsumerRecords<>(Collections.emptyMap()));

        List<Map<String, Object>> result = tools.kafkaConsume("topic", 1, 100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("key", "k").containsEntry("value", "v");
    }

    @Test
    void kafkaConsume_nullKeyAndValue_replacedWithEmpty() {
        when(consumerFactoryProvider.getIfAvailable()).thenReturn(consumerFactory);
        when(consumerFactory.createConsumer(anyString(), anyString())).thenReturn(consumer);

        TopicPartition tp = new TopicPartition("topic", 0);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0, null, null);
        ConsumerRecords<String, String> batch = new ConsumerRecords<>(Map.of(tp, List.of(record)));
        when(consumer.poll(any(Duration.class))).thenReturn(batch, new ConsumerRecords<>(Collections.emptyMap()));

        List<Map<String, Object>> result = tools.kafkaConsume("topic", 1, 100L);

        assertThat(result.get(0)).containsEntry("key", "").containsEntry("value", "");
    }

    @Test
    void kafkaConsume_defaultMaxAndTimeout_uses10and3000() {
        when(consumerFactoryProvider.getIfAvailable()).thenReturn(consumerFactory);
        when(consumerFactory.createConsumer(anyString(), anyString())).thenReturn(consumer);
        when(consumer.poll(any(Duration.class))).thenReturn(new ConsumerRecords<>(Collections.emptyMap()));

        // Should complete without exception using defaults
        List<Map<String, Object>> result = tools.kafkaConsume("topic", null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void kafkaConsume_multipleRecordsBelowMax_collectsAllWithoutBreaking() {
        // Covers the for-loop fall-through path (out.size() < max after adding a record)
        when(consumerFactoryProvider.getIfAvailable()).thenReturn(consumerFactory);
        when(consumerFactory.createConsumer(anyString(), anyString())).thenReturn(consumer);

        TopicPartition tp = new TopicPartition("topic", 0);
        ConsumerRecord<String, String> r1 = new ConsumerRecord<>("topic", 0, 0, "k1", "v1");
        ConsumerRecord<String, String> r2 = new ConsumerRecord<>("topic", 0, 1, "k2", "v2");
        ConsumerRecords<String, String> batch = new ConsumerRecords<>(Map.of(tp, List.of(r1, r2)));

        when(consumer.poll(any(Duration.class))).thenReturn(batch, new ConsumerRecords<>(Collections.emptyMap()));

        List<Map<String, Object>> result = tools.kafkaConsume("topic", 5, 100L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("key", "k1");
        assertThat(result.get(1)).containsEntry("key", "k2");
    }
}
