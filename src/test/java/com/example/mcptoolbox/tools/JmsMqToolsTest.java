package com.example.mcptoolbox.tools;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JmsMqToolsTest {

    @Mock ObjectProvider<JmsTemplate> provider;
    @Mock JmsTemplate jms;

    private JmsMqTools tools;

    @BeforeEach
    void setUp() {
        tools = new JmsMqTools(provider);
    }

    // --- jms() throws when not configured ---

    @Test
    void mqSend_noJms_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.mqSend("Q", "msg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JMS/MQ not configured");
    }

    // --- mqSend ---

    @Test
    void mqSend_delegatesAndReturnsConfirmation() {
        when(provider.getIfAvailable()).thenReturn(jms);

        String result = tools.mqSend("MY.QUEUE", "hello");

        verify(jms).convertAndSend("MY.QUEUE", "hello");
        assertThat(result).contains("MY.QUEUE");
    }

    // --- mqReceive ---

    @Test
    void mqReceive_noJms_throwsIllegalState() {
        when(provider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> tools.mqReceive("Q", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mqReceive_textMessage_returnsBody() throws JMSException {
        when(provider.getIfAvailable()).thenReturn(jms);
        TextMessage textMsg = mock(TextMessage.class);
        when(textMsg.getText()).thenReturn("hello world");
        when(jms.receive("Q")).thenReturn(textMsg);

        String result = tools.mqReceive("Q", 5000L);

        verify(jms).setReceiveTimeout(5000L);
        assertThat(result).isEqualTo("hello world");
    }

    @Test
    void mqReceive_defaultTimeout_uses3000ms() throws JMSException {
        when(provider.getIfAvailable()).thenReturn(jms);
        TextMessage textMsg = mock(TextMessage.class);
        when(textMsg.getText()).thenReturn("msg");
        when(jms.receive("Q")).thenReturn(textMsg);

        tools.mqReceive("Q", null);

        verify(jms).setReceiveTimeout(3000L);
    }

    @Test
    void mqReceive_nullMessage_returnsNull() {
        when(provider.getIfAvailable()).thenReturn(jms);
        when(jms.receive("Q")).thenReturn(null);

        String result = tools.mqReceive("Q", null);

        assertThat(result).isNull();
    }

    @Test
    void mqReceive_nonTextMessage_returnsToString() throws JMSException {
        when(provider.getIfAvailable()).thenReturn(jms);
        Message msg = mock(Message.class);
        when(msg.toString()).thenReturn("some-msg-repr");
        when(jms.receive("Q")).thenReturn(msg);

        String result = tools.mqReceive("Q", null);

        assertThat(result).isEqualTo("some-msg-repr");
    }

    @Test
    void mqReceive_jmsExceptionOnGetText_throwsRuntimeException() throws JMSException {
        when(provider.getIfAvailable()).thenReturn(jms);
        TextMessage textMsg = mock(TextMessage.class);
        when(textMsg.getText()).thenThrow(new JMSException("read error"));
        when(jms.receive("Q")).thenReturn(textMsg);

        assertThatThrownBy(() -> tools.mqReceive("Q", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to receive from MQ");
    }
}
