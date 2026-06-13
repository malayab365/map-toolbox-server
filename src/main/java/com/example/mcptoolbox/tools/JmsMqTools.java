package com.example.mcptoolbox.tools;

import jakarta.jms.TextMessage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * MCP tools for JMS / IBM MQ messaging, backed by {@link JmsTemplate}.
 *
 * <p>Configured under {@code ibm.mq.*} (mq-jms-spring-boot-starter) or any other
 * JMS ConnectionFactory on the classpath. Injected lazily so the server starts
 * without an MQ connection.
 */
@Component
public class JmsMqTools {

    private final ObjectProvider<JmsTemplate> jmsProvider;

    public JmsMqTools(ObjectProvider<JmsTemplate> jmsProvider) {
        this.jmsProvider = jmsProvider;
    }

    private JmsTemplate jms() {
        JmsTemplate t = jmsProvider.getIfAvailable();
        if (t == null) {
            throw new IllegalStateException(
                    "JMS/MQ not configured. Set ibm.mq.* (or another JMS ConnectionFactory) in application.yml.");
        }
        return t;
    }

    @Tool(description = "Send a text message to a JMS/IBM MQ queue.")
    public String mqSend(
            @ToolParam(description = "Destination queue name.") String queue,
            @ToolParam(description = "Text payload to send.") String message) {
        jms().convertAndSend(queue, message);
        return "Sent to queue " + queue;
    }

    @Tool(description = "Receive a single text message from a JMS/IBM MQ queue, waiting up to the given timeout. Returns the body or null if none available.")
    public String mqReceive(
            @ToolParam(description = "Destination queue name.") String queue,
            @ToolParam(required = false, description = "Receive timeout in milliseconds (default 3000).") Long timeoutMs) {
        JmsTemplate template = jms();
        template.setReceiveTimeout(timeoutMs == null ? 3000L : timeoutMs);
        try {
            jakarta.jms.Message msg = template.receive(queue);
            if (msg instanceof TextMessage text) {
                return text.getText();
            }
            return msg == null ? null : msg.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to receive from MQ: " + e.getMessage(), e);
        }
    }
}
