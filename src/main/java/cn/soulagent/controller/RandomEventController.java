package cn.soulagent.controller;

import cn.soulagent.entity.RandomEvent;
import cn.soulagent.service.RandomEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/random-event")
@RequiredArgsConstructor
public class RandomEventController {

    private final RandomEventService randomEventService;

    @GetMapping("/character/{characterId}/unshared")
    public ResponseEntity<Map<String, Object>> getUnsharedEvents(@PathVariable Long characterId) {
        List<RandomEvent> events = randomEventService.getUnsharedEvents(characterId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("events", events);
        response.put("count", events.size());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/character/{characterId}/recent")
    public ResponseEntity<Map<String, Object>> getRecentEvents(
            @PathVariable Long characterId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RandomEvent> events = randomEventService.getRecentEvents(characterId, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("events", events);
        response.put("count", events.size());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/share")
    public ResponseEntity<Map<String, String>> markAsShared(@PathVariable Long eventId) {
        randomEventService.markAsShared(eventId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "success");
        
        return ResponseEntity.ok(response);
    }
}
