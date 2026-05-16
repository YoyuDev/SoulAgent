package cn.soulagent.service;

import cn.soulagent.entity.AppSetting;
import cn.soulagent.entity.Personality;
import cn.soulagent.entity.RandomEvent;
import cn.soulagent.entity.SoulCharacter;
import cn.soulagent.mapper.AppSettingMapper;
import cn.soulagent.mapper.PersonalityMapper;
import cn.soulagent.mapper.RandomEventMapper;
import cn.soulagent.mapper.SoulCharacterMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RandomEventService {

    private static final Logger log = LoggerFactory.getLogger(RandomEventService.class);

    private static final long EVENT_INTERVAL_HOURS = 6;

    private static final String[] EVENT_TYPES = {
            "daily_life", "emotion", "memory", "question", "observation", "activity"
    };

    private final RandomEventMapper eventMapper;
    private final SoulCharacterMapper characterMapper;
    private final PersonalityMapper personalityMapper;
    private final AiModelFactory aiModelFactory;
    private final AppSettingMapper appSettingMapper;

    public void checkAndGenerateEvents() {
        List<SoulCharacter> characters = characterMapper.selectList(null);
        for (SoulCharacter character : characters) {
            if (character.getRandomEventEnabled() == null || character.getRandomEventEnabled() == 0) {
                continue;
            }

            long now = System.currentTimeMillis();
            long lastEventTime = character.getLastEventTime() != null ? character.getLastEventTime() : 0;
            long hoursSinceLastEvent = (now - lastEventTime) / (1000 * 60 * 60);

            if (hoursSinceLastEvent >= EVENT_INTERVAL_HOURS) {
                generateEvent(character);
            }
        }
    }

    private void generateEvent(SoulCharacter character) {
        try {
            String eventType = EVENT_TYPES[new Random().nextInt(EVENT_TYPES.length)];
            String eventContent = generateEventContent(character, eventType);

            if (eventContent != null && !eventContent.isEmpty()) {
                RandomEvent event = new RandomEvent();
                event.setCharacterId(character.getId());
                event.setEventType(eventType);
                event.setEventContent(eventContent);
                event.setEventTime(System.currentTimeMillis());
                event.setIsShared(0);
                event.setShareTime(0L);

                eventMapper.insert(event);

                character.setLastEventTime(System.currentTimeMillis());
                characterMapper.updateById(character);

                log.info("为角色 {} 生成随机事件：{} - {}", character.getName(), eventType,
                        eventContent.substring(0, Math.min(20, eventContent.length())));
            }
        } catch (Exception e) {
            log.warn("为角色 {} 生成随机事件失败：{}", character.getName(), e.getMessage());
        }
    }

    private String generateEventContent(SoulCharacter character, String eventType) {
        Personality personality = getPersonality(character.getId());
        if (personality == null) {
            return null;
        }

        String timeContext = getTimeContext();
        String prompt = buildEventPrompt(character, personality, eventType, timeContext);

        try {
            String apiKey = getSetting("apiKey");
            String apiUrl = getSetting("apiUrl");
            String modelName = getSetting("modelName");

            ChatModel model = aiModelFactory.chatModel(apiKey, apiUrl, modelName);
            String response = model.chat(UserMessage.from(prompt)).aiMessage().text();

            if (shouldShare(character, personality, eventType)) {
                return response + " [想分享给你]";
            } else {
                return response + " [暂时不想分享]";
            }
        } catch (Exception e) {
            log.warn("生成事件内容失败：{}", e.getMessage());
            return null;
        }
    }

    private String buildEventPrompt(SoulCharacter character, Personality personality,
                                     String eventType, String timeContext) {
        String traits = personality.getTraits();
        String style = personality.getSpeakingStyle();
        String emotion = personality.getEmotionBaseline();

        String eventInstruction = switch (eventType) {
            case "daily_life" -> "描述一个日常生活场景（如吃饭、走路、看到某个东西）";
            case "emotion" -> "描述一个情绪波动或感受";
            case "memory" -> "回忆一件过去的事情";
            case "question" -> "提出一个想问对方的问题";
            case "observation" -> "描述一个观察到的现象或细节";
            case "activity" -> "描述正在进行或计划进行的活动";
            default -> "描述一个生活片段";
        };

        return """
            你是「%s」，一个真实的人。
            
            【你的性格】
            %s
            
            【你说话的方式】
            %s
            
            【你的情绪基调】
            %s
            
            现在是%s。
            
            %s
            
            要求：
            - 用第一人称"我"来描述
            - 符合你的性格和说话风格
            - 简短、口语化，像发微信一样（30-80 字）
            - 不要解释，直接描述
            
            内容：
            """.formatted(
                character.getName(),
                traits,
                style,
                emotion,
                timeContext,
                eventInstruction
        );
    }

    private boolean shouldShare(SoulCharacter character, Personality personality, String eventType) {
        String traits = personality.getTraits() != null ? personality.getTraits().toLowerCase() : "";
        String style = personality.getSpeakingStyle() != null ? personality.getSpeakingStyle().toLowerCase() : "";

        String[] extrovertedWords = {"外向", "开朗", "活泼", "热情", "话多", "分享", "表达", "社交"};
        String[] introvertedWords = {"内向", "安静", "低调", "不善表达", "独处", "思考"};

        int extrovertedScore = 0;
        for (String word : extrovertedWords) {
            if (traits.contains(word) || style.contains(word)) {
                extrovertedScore++;
            }
        }

        int introvertedScore = 0;
        for (String word : introvertedWords) {
            if (traits.contains(word) || style.contains(word)) {
                introvertedScore++;
            }
        }

        double baseShareRate = 0.5 + (extrovertedScore - introvertedScore) * 0.1;

        double timeFactor = 1.0;
        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(22, 0)) || now.isBefore(LocalTime.of(6, 0))) {
            timeFactor = 0.7;
        }

        double eventFactor = switch (eventType) {
            case "emotion", "question" -> 1.2;
            case "memory" -> 0.9;
            default -> 1.0;
        };

        double shareRate = baseShareRate * timeFactor * eventFactor;
        double random = new Random().nextDouble();

        return random < shareRate;
    }

    private String getTimeContext() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(6, 0))) {
            return "凌晨";
        } else if (now.isBefore(LocalTime.of(11, 0))) {
            return "上午";
        } else if (now.isBefore(LocalTime.of(14, 0))) {
            return "中午";
        } else if (now.isBefore(LocalTime.of(18, 0))) {
            return "下午";
        } else if (now.isBefore(LocalTime.of(22, 0))) {
            return "晚上";
        } else {
            return "深夜";
        }
    }

    private Personality getPersonality(Long characterId) {
        return personalityMapper.selectOne(
                new QueryWrapper<Personality>().eq("character_id", characterId)
        );
    }

    private String getSetting(String key) {
        AppSetting setting = appSettingMapper.selectOne(
                new QueryWrapper<AppSetting>().eq("setting_key", key)
        );
        return setting != null ? setting.getSettingValue() : null;
    }

    public List<RandomEvent> getUnsharedEvents(Long characterId) {
        QueryWrapper<RandomEvent> qw = new QueryWrapper<>();
        qw.eq("character_id", characterId)
                .eq("is_shared", 0)
                .orderByDesc("event_time");
        return eventMapper.selectList(qw);
    }

    public void markAsShared(Long eventId) {
        RandomEvent event = eventMapper.selectById(eventId);
        if (event != null) {
            event.setIsShared(1);
            event.setShareTime(System.currentTimeMillis());
            eventMapper.updateById(event);
        }
    }

    public List<RandomEvent> getRecentEvents(Long characterId, int limit) {
        QueryWrapper<RandomEvent> qw = new QueryWrapper<>();
        qw.eq("character_id", characterId)
                .orderByDesc("event_time")
                .last("LIMIT " + limit);
        return eventMapper.selectList(qw);
    }
}