package com.tyson.digitalwallet.ddd.infrastructure.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyson.digitalwallet.ddd.infrastructure.cached.RedisInfrasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Triển khai ChatMemoryRepository sử dụng Redis phân tán.
 * Hỗ trợ lưu trữ lịch sử hội thoại AI đa phiên, tự động dọn dẹp theo thời gian sống (TTL)
 * và chia sẻ ngữ cảnh an toàn giữa nhiều node/pod Spring Boot.
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryRepository.class);
    private static final String KEY_PREFIX = "ai:chat:memory:";
    private static final long DEFAULT_TTL_MINUTES = 60; // 1 giờ không hoạt động tự động giải phóng Redis

    private final RedisInfrasService redisInfrasService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(RedisInfrasService redisInfrasService,
                                     StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper) {
        this.redisInfrasService = redisInfrasService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private String buildKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    @Override
    public List<String> findConversationIds() {
        try {
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return List.of();
            }
            return keys.stream()
                    .map(k -> k.substring(KEY_PREFIX.length()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to retrieve conversation IDs from Redis: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = buildKey(conversationId);
        String json = redisInfrasService.get(key);
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<ChatMessageDto> dtos = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ChatMessageDto.class)
            );
            if (dtos == null) {
                return List.of();
            }
            return dtos.stream().map(this::fromDto).toList();
        } catch (Exception e) {
            log.warn("Failed to deserialize chat messages for conversation: {}. Evicting corrupted cache.", conversationId, e);
            redisInfrasService.delete(key);
            return List.of();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            deleteByConversationId(conversationId);
            return;
        }

        String key = buildKey(conversationId);
        List<ChatMessageDto> dtos = messages.stream().map(this::toDto).toList();
        try {
            String json = objectMapper.writeValueAsString(dtos);
            redisInfrasService.set(key, json, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to serialize chat messages to Redis for conversation: {}", conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        String key = buildKey(conversationId);
        redisInfrasService.delete(key);
        log.debug("Deleted AI chat conversation memory from Redis: {}", conversationId);
    }

    // ================= DTO & Mapping =================

    public record ChatMessageDto(
            String type,
            String content,
            Map<String, Object> metadata,
            List<ToolCallDto> toolCalls,
            List<ToolResponseDto> toolResponses
    ) {
        public record ToolCallDto(String id, String type, String name, String arguments) {}
        public record ToolResponseDto(String id, String name, String responseData) {}
    }

    private ChatMessageDto toDto(Message message) {
        String type = message.getMessageType() != null ? message.getMessageType().name() : "USER";
        String content = message.getText();
        Map<String, Object> metadata = message.getMetadata() != null ? new HashMap<>(message.getMetadata()) : Map.of();

        List<ChatMessageDto.ToolCallDto> toolCalls = null;
        if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
            toolCalls = assistantMessage.getToolCalls().stream()
                    .map(tc -> new ChatMessageDto.ToolCallDto(tc.id(), tc.type(), tc.name(), tc.arguments()))
                    .toList();
        }

        List<ChatMessageDto.ToolResponseDto> toolResponses = null;
        if (message instanceof ToolResponseMessage toolResponseMessage && toolResponseMessage.getResponses() != null) {
            toolResponses = toolResponseMessage.getResponses().stream()
                    .map(tr -> new ChatMessageDto.ToolResponseDto(tr.id(), tr.name(), tr.responseData()))
                    .toList();
        }

        return new ChatMessageDto(type, content, metadata, toolCalls, toolResponses);
    }

    private Message fromDto(ChatMessageDto dto) {
        MessageType type;
        try {
            type = MessageType.valueOf(dto.type());
        } catch (Exception e) {
            type = MessageType.USER;
        }

        String content = dto.content() != null ? dto.content() : "";
        Map<String, Object> metadata = dto.metadata() != null ? dto.metadata() : Map.of();

        return switch (type) {
            case USER -> UserMessage.builder().text(content).metadata(metadata).build();
            case SYSTEM -> SystemMessage.builder().text(content).metadata(metadata).build();
            case ASSISTANT -> {
                var builder = AssistantMessage.builder().content(content).properties(metadata);
                if (dto.toolCalls() != null && !dto.toolCalls().isEmpty()) {
                    var calls = dto.toolCalls().stream()
                            .map(tc -> new AssistantMessage.ToolCall(tc.id(), tc.type(), tc.name(), tc.arguments()))
                            .toList();
                    builder.toolCalls(calls);
                }
                yield builder.build();
            }
            case TOOL -> {
                var builder = ToolResponseMessage.builder().metadata(metadata);
                if (dto.toolResponses() != null && !dto.toolResponses().isEmpty()) {
                    var resps = dto.toolResponses().stream()
                            .map(tr -> new ToolResponseMessage.ToolResponse(tr.id(), tr.name(), tr.responseData()))
                            .toList();
                    builder.responses(resps);
                }
                yield builder.build();
            }
        };
    }
}
