package com.example.mcptoolbox.tools;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MCP tools for Apache Kafka, backed by spring-kafka.
 *
 * <p>Broker and (de)serializer settings are configured under {@code spring.kafka.*}.
 * The producer publishes String key/value records; {@link #kafkaConsume} polls a
 * topic on demand using a fresh consumer in its own group.
 */
@Component
public class KafkaTools {

    private final ObjectProvider<KafkaTemplate<String, String>> templateProvider;
    private final ObjectProvider<ConsumerFactory<String, String>> consumerFactoryProvider;

    public KafkaTools(ObjectProvider<KafkaTemplate<String, String>> templateProvider,
                      ObjectProvider<ConsumerFactory<String, String>> consumerFactoryProvider) {
        this.templateProvider = templateProvider;
        this.consumerFactoryProvider = consumerFactoryProvider;
    }

    @Tool(description = "Publish a message to a Kafka topic. Returns the partition and offset the record landed on.")
    public String kafkaPublish(
            @ToolParam(description = "Kafka topic name.") String topic,
            @ToolParam(description = "Message value (string payload).") String message,
            @ToolParam(required = false, description = "Optional message key for partitioning.") String key) {
        KafkaTemplate<String, String> template = templateProvider.getIfAvailable();
        if (template == null) {
            throw new IllegalStateException("Kafka not configured. Set spring.kafka.bootstrap-servers.");
        }
        var result = (key == null || key.isBlank())
                ? template.send(topic, message)
                : template.send(topic, key, message);
        try {
            var metadata = result.get().getRecordMetadata();
            return "Sent to %s partition=%d offset=%d".formatted(
                    metadata.topic(), metadata.partition(), metadata.offset());
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to Kafka: " + e.getMessage(), e);
        }
    }

    @Tool(description = "Consume up to N messages from the start of a Kafka topic (polls once). Returns records with partition, offset, key and value.")
    public List<Map<String, Object>> kafkaConsume(
            @ToolParam(description = "Kafka topic name.") String topic,
            @ToolParam(required = false, description = "Maximum number of records to return (default 10).") Integer maxRecords,
            @ToolParam(required = false, description = "Poll timeout in milliseconds (default 3000).") Long pollTimeoutMs) {
        ConsumerFactory<String, String> factory = consumerFactoryProvider.getIfAvailable();
        if (factory == null) {
            throw new IllegalStateException("Kafka not configured. Set spring.kafka.bootstrap-servers.");
        }
        int max = maxRecords == null ? 10 : maxRecords;
        long timeout = pollTimeoutMs == null ? 3000L : pollTimeoutMs;

        List<Map<String, Object>> out = new ArrayList<>();
        // Unique group so we read from the configured auto.offset.reset point each call.
        String group = "mcp-toolbox-" + System.currentTimeMillis();
        try (Consumer<String, String> consumer =
                     factory.createConsumer(group, "mcp-toolbox-client")) {
            consumer.subscribe(Collections.singletonList(topic));
            long deadline = System.currentTimeMillis() + timeout;
            while (out.size() < max && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    out.add(Map.of(
                            "partition", r.partition(),
                            "offset", r.offset(),
                            "key", r.key() == null ? "" : r.key(),
                            "value", r.value() == null ? "" : r.value()));
                    if (out.size() >= max) {
                        break;
                    }
                }
            }
        }
        return out;
    }
}
