package com.tyson.digitalwallet.ddd.infrastructure.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.infrastructure.cached.RedisInfrasService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RedisChatMemoryRepositoryTest {

    private RedisInfrasService redisInfrasService;
    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;
    private RedisChatMemoryRepository repository;

    @Before
    public void setUp() {
        redisInfrasService = mock(RedisInfrasService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        objectMapper = new ObjectMapper();
        repository = new RedisChatMemoryRepository(redisInfrasService, redisTemplate, objectMapper);
    }

    @Test
    public void saveAllAndFindByConversationIdSuccess() {
        String convId = "conv-123";
        String expectedKey = "ai:chat:memory:" + convId;

        List<Message> messages = List.of(
                new UserMessage("Số dư ví tôi là bao nhiêu?"),
                new AssistantMessage("Số dư của bạn là 500,000 VNĐ.")
        );

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        repository.saveAll(convId, messages);

        verify(redisInfrasService).set(eq(expectedKey), jsonCaptor.capture(), eq(60L), eq(TimeUnit.MINUTES));
        String savedJson = jsonCaptor.getValue();
        assertNotNull(savedJson);
        assertTrue(savedJson.contains("Số dư ví tôi là bao nhiêu?"));

        // Giả lập đọc lại từ Redis
        when(redisInfrasService.get(expectedKey)).thenReturn(savedJson);

        List<Message> retrieved = repository.findByConversationId(convId);
        assertEquals(2, retrieved.size());
        assertEquals(MessageType.USER, retrieved.get(0).getMessageType());
        assertEquals("Số dư ví tôi là bao nhiêu?", retrieved.get(0).getText());
        assertEquals(MessageType.ASSISTANT, retrieved.get(1).getMessageType());
        assertEquals("Số dư của bạn là 500,000 VNĐ.", retrieved.get(1).getText());
    }

    @Test
    public void deleteByConversationIdCallsRedisDelete() {
        String convId = "conv-456";
        String expectedKey = "ai:chat:memory:" + convId;

        repository.deleteByConversationId(convId);

        verify(redisInfrasService).delete(expectedKey);
    }

    @Test
    public void findConversationIdsReturnsStrippedKeys() {
        when(redisTemplate.keys("ai:chat:memory:*")).thenReturn(Set.of("ai:chat:memory:conv-1", "ai:chat:memory:conv-2"));

        List<String> ids = repository.findConversationIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains("conv-1"));
        assertTrue(ids.contains("conv-2"));
    }
}
