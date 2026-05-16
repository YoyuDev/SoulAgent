package cn.soulagent.controller;

import cn.soulagent.entity.AppSetting;
import cn.soulagent.entity.SoulCharacter;
import cn.soulagent.mapper.AppSettingMapper;
import cn.soulagent.service.CharacterService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/character")
@RequiredArgsConstructor
public class CharacterController {

    private static final Logger log = LoggerFactory.getLogger(CharacterController.class);

    private final CharacterService service;
    private final AppSettingMapper appSettingMapper;
    private final ExecutorService taskExecutor;

    @GetMapping("/list")
    public List<SoulCharacter> list() {
        return service.list();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PutMapping("/{id}/random-event")
    public void updateRandomEvent(@PathVariable Long id, @RequestBody RandomEventConfig config) {
        service.updateRandomEventEnabled(id, config.getEnabled());
    }

    @PostMapping(value = "/create", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter create(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String chatData,
            @RequestParam(required = false) MultipartFile avatar,
            @RequestParam(defaultValue = "0") Integer randomEventEnabled
    ) {
        SseEmitter emitter = new SseEmitter(300_000L);

        String apiKey = getSetting("apiKey");
        String apiUrl = getSetting("apiUrl");
        String modelName = getSetting("modelName");
        String embeddingApiKey = getSetting("embeddingApiKey");
        String embeddingApiUrl = getSetting("embeddingApiUrl");
        String embeddingModelName = getSetting("embeddingModelName");

        taskExecutor.submit(() -> {
            try {
                Long id = service.create(name, description, chatData, avatar, randomEventEnabled,
                        apiKey, apiUrl, modelName, embeddingApiKey, embeddingApiUrl, embeddingModelName,
                        (msg, pct) -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("progress")
                                        .data("{\"message\":\"" + msg + "\",\"percent\":" + pct + "}"));
                            } catch (Exception e) {
                                log.debug("SSE send progress failed: {}", e.getMessage());
                            }
                        });

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"id\":" + id + "}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Character create error: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}"));
                } catch (Exception ex) {
                    log.debug("SSE send error failed: {}", ex.getMessage());
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private String getSetting(String key) {
        AppSetting setting = appSettingMapper.selectOne(
                new QueryWrapper<AppSetting>().eq("setting_key", key)
        );
        return setting != null ? setting.getSettingValue() : null;
    }
}
