package cn.soulagent.controller;

import cn.soulagent.entity.SoulCharacter;
import cn.soulagent.service.CharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/character")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService service;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping("/list")
    public List<SoulCharacter> list() {
        return service.list();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping(value = "/create", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter create(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String chatData,
            @RequestParam(required = false) MultipartFile avatar,
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false) String apiUrl,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String embeddingApiUrl,
            @RequestParam(required = false) String embeddingApiKey,
            @RequestParam(required = false) String embeddingModelName
    ) {
        SseEmitter emitter = new SseEmitter(300_000L);

        executor.submit(() -> {
            try {
                Long id = service.create(name, description, chatData, avatar,
                        apiKey, apiUrl, modelName, embeddingApiKey, embeddingApiUrl, embeddingModelName,
                        (msg, pct) -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("progress")
                                        .data("{\"message\":\"" + msg + "\",\"percent\":" + pct + "}"));
                            } catch (Exception ignored) {}
                        });

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data("{\"id\":" + id + "}"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":\"" + e.getMessage().replace("\"", "'") + "\"}"));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
