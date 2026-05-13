package cn.soulagent.skill;

import cn.soulagent.entity.Personality;
import cn.soulagent.entity.SoulCharacter;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SkillContext {

    private final Long characterId;
    private final SoulCharacter character;
    private final Personality personality;
    private final String userMessage;
    private final List<String> recentHistory;
    private final List<String> memories;

    private final String apiKey;
    private final String apiUrl;
    private final String modelName;
    private final String embeddingApiKey;
    private final String embeddingApiUrl;
    private final String embeddingModelName;
}