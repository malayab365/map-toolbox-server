package com.example.mcptoolbox;

import com.example.mcptoolbox.tools.JsonTools;
import com.example.mcptoolbox.tools.KafkaTools;
import com.example.mcptoolbox.tools.MongoTools;
import com.example.mcptoolbox.tools.OracleDbTools;
import com.example.mcptoolbox.tools.RedisTools;
import com.example.mcptoolbox.tools.JmsMqTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Entry point for the MCP Toolbox server.
 *
 * <p>Exposes a set of MCP tools that connect to common backend systems:
 * Oracle (JDBC), MongoDB, Redis, Kafka, JSON utilities and JMS/IBM MQ.
 *
 * <p>Transports:
 * <ul>
 *   <li>SSE/HTTP — enabled by default (WebMVC starter), endpoint /sse + /mcp/message</li>
 *   <li>STDIO — enable with spring.ai.mcp.server.stdio=true (see application-stdio.yml)</li>
 * </ul>
 */
@SpringBootApplication
public class McpToolboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpToolboxApplication.class, args);
    }

    /**
     * Registers every {@code @Tool}-annotated method on the given beans as MCP tools.
     */
    @Bean
    public ToolCallbackProvider toolboxTools(OracleDbTools oracle,
                                             MongoTools mongo,
                                             RedisTools redis,
                                             KafkaTools kafka,
                                             JsonTools json,
                                             JmsMqTools mq) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(oracle, mongo, redis, kafka, json, mq)
                .build();
    }
}
