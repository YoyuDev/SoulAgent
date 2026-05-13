package cn.soulagent.controller;

import cn.soulagent.dto.ChatRequest;
import cn.soulagent.entity.AppSetting;
import cn.soulagent.entity.ChatMessage;
import cn.soulagent.mapper.AppSettingMapper;
import cn.soulagent.mapper.ChatMessageMapper;
import cn.soulagent.service.ChatService;
import cn.soulagent.service.RedisService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final RedisService redisService;
    private final ChatMessageMapper chatMessageMapper;
    private final AppSettingMapper appSettingMapper;
    private final ExecutorService taskExecutor;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(120_000L);

        taskExecutor.submit(() -> {
            try {
                chatService.chatStream(req,
                        token -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data("{\"content\":\"" + escapeJson(token) + "\"}"));
                            } catch (Exception e) {
                                log.debug("SSE send token failed: {}", e.getMessage());
                            }
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data("{}"));
                            } catch (Exception e) {
                                log.debug("SSE send done failed: {}", e.getMessage());
                            }
                        }
                );
                emitter.complete();
            } catch (Exception e) {
                log.error("Chat error: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                } catch (Exception ex) {
                    log.debug("SSE send error failed: {}", ex.getMessage());
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @DeleteMapping("/chat/history/{characterId}")
    public void clearHistory(@PathVariable Long characterId) {
        redisService.clear(characterId);
        chatMessageMapper.delete(
                new QueryWrapper<ChatMessage>().eq("character_id", characterId)
        );
    }

    @GetMapping("/chat/history/{characterId}")
    public Map<String, Object> getHistory(
            @PathVariable Long characterId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);

        QueryWrapper<ChatMessage> qw = new QueryWrapper<ChatMessage>()
                .eq("character_id", characterId);

        if (before != null) {
            qw.lt("id", before);
        }

        qw.orderByDesc("id");

        List<ChatMessage> allMessages = chatMessageMapper.selectList(qw);

        boolean hasMore = allMessages.size() > safeSize;
        List<ChatMessage> messages = allMessages.size() > safeSize
                ? allMessages.subList(0, safeSize)
                : allMessages;

        java.util.Collections.reverse(messages);

        return Map.of(
                "messages", messages,
                "hasMore", hasMore
        );
    }

    @GetMapping("/settings")
    public Map<String, String> getSettings() {
        Map<String, String> result = new java.util.HashMap<>();
        appSettingMapper.selectList(null).forEach(s ->
                result.put(s.getSettingKey(), s.getSettingValue())
        );
        return result;
    }

    @PostMapping("/settings")
    public void saveSettings(@RequestBody Map<String, String> settings) {
        settings.forEach((key, value) -> {
            AppSetting existing = appSettingMapper.selectOne(
                    new QueryWrapper<AppSetting>().eq("setting_key", key)
            );
            if (existing != null) {
                existing.setSettingValue(value);
                appSettingMapper.updateById(existing);
            } else {
                AppSetting s = new AppSetting();
                s.setSettingKey(key);
                s.setSettingValue(value);
                appSettingMapper.insert(s);
            }
        });
    }
}
