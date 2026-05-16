package cn.soulagent.controller;

import cn.soulagent.entity.CharacterRelationship;
import cn.soulagent.service.RelationshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/relationship")
@RequiredArgsConstructor
public class RelationshipController {

    private final RelationshipService relationshipService;

    @GetMapping("/{characterId}")
    public ResponseEntity<Map<String, Object>> getRelationship(@PathVariable Long characterId) {
        CharacterRelationship relationship = relationshipService.get(characterId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("intimacyScore", relationship.getIntimacyScore());
        response.put("trustScore", relationship.getTrustScore());
        response.put("relationshipStage", relationship.getRelationshipStage());
        response.put("stageDesc", relationshipService.getStageDescription(characterId));
        response.put("totalMessages", relationship.getTotalMessages());
        response.put("firstChatTime", relationship.getFirstChatTime());
        response.put("lastChatTime", relationship.getLastChatTime());
        
        return ResponseEntity.ok(response);
    }
}
