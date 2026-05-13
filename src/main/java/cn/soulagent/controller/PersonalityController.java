package cn.soulagent.controller;

import cn.soulagent.entity.Personality;
import cn.soulagent.service.PersonalityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/personality")
@RequiredArgsConstructor
public class PersonalityController {

    private final PersonalityService personalityService;

    @GetMapping("/{characterId}")
    public Map<String, String> getPersonality(@PathVariable Long characterId) {
        Personality p = personalityService.get(characterId);
        if (p == null) {
            return Map.of("emotion", "", "emotionBaseline", "");
        }
        return Map.of(
                "emotion", p.getCurrentEmotion() != null ? p.getCurrentEmotion() : "",
                "emotionBaseline", p.getEmotionBaseline() != null ? p.getEmotionBaseline() : ""
        );
    }
}