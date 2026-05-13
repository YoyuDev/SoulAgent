package cn.soulagent.controller;

import cn.soulagent.entity.CharacterRelationship;
import cn.soulagent.entity.Personality;
import cn.soulagent.service.PersonalityService;
import cn.soulagent.service.RelationshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/personality")
@RequiredArgsConstructor
public class PersonalityController {

    private final PersonalityService personalityService;
    private final RelationshipService relationshipService;

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

    @GetMapping("/relationship/{characterId}")
    public Map<String, Object> getRelationship(@PathVariable Long characterId) {
        CharacterRelationship rel = relationshipService.get(characterId);
        if (rel == null) {
            return Map.of(
                    "stage", "stranger",
                    "stageDesc", "陌生人",
                    "intimacyScore", 0.0,
                    "trustScore", 0.0,
                    "totalMessages", 0L
            );
        }
        return Map.of(
                "stage", rel.getRelationshipStage(),
                "stageDesc", formatStage(rel.getRelationshipStage()),
                "intimacyScore", rel.getIntimacyScore() != null ? rel.getIntimacyScore() : 0.0,
                "trustScore", rel.getTrustScore() != null ? rel.getTrustScore() : 0.0,
                "totalMessages", rel.getTotalMessages() != null ? rel.getTotalMessages() : 0L
        );
    }

    private String formatStage(String stage) {
        return switch (stage) {
            case "stranger" -> "陌生人";
            case "acquaintance" -> "认识的人";
            case "friend" -> "朋友";
            case "close_friend" -> "好朋友";
            case "intimate" -> "亲密的人";
            default -> "陌生人";
        };
    }
}