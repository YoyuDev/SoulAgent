package cn.soulagent.service;

import cn.soulagent.entity.ConversationSummary;
import cn.soulagent.entity.Personality;
import cn.soulagent.mapper.ConversationSummaryMapper;
import cn.soulagent.mapper.PersonalityMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalityService {

    private static final Logger log = LoggerFactory.getLogger(PersonalityService.class);

    private static final int EVOLUTION_INTERVAL = 50;

    private final PersonalityMapper mapper;
    private final AiModelFactory aiModelFactory;
    private final ObjectMapper objectMapper;
    private final ConversationSummaryMapper summaryMapper;

    public Personality generate(List<String> msgs, String apiKey, String apiUrl, String modelName) {

        String prompt = """
请严格只输出一个JSON对象，不要输出任何其他文字、解释、注释或markdown标记。JSON不要有尾部逗号，所有字符串值必须用双引号闭合。

你是一个人格分析专家。根据以下聊天记录，深度分析"对方"（即聊天中非"我"的那一方）的人格特征。
要求：
- traits：必须详细描述此人的性格特点、价值观、兴趣爱好、口头禅、说话习惯、可能的年龄/性别/职业推断，至少100字
- speaking_style：详细描述此人的语言风格，包括：用词偏好、句式特点、是否用表情/emoji、语气词习惯、标点使用习惯等，至少50字
- emotion_baseline：描述此人的情绪基线和情绪表达方式
- common_phrases：列出此人经常说的口头禅或典型表达，用 | 分隔，至少5个

格式：
{"traits":"详细性格描述","speaking_style":"详细说话风格","emotion_baseline":"情绪基线","common_phrases":"口头禅1|口头禅2|口头禅3"}

以下是聊天记录：
%s
""".formatted(String.join("\n", msgs));

        ChatModel model = aiModelFactory.jsonChatModel(apiKey, apiUrl, modelName);
        String res = model.chat(UserMessage.from(prompt)).aiMessage().text();

        try {
            String json = res.trim();
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?s)```(?:json)?\\s*(.+?)```", java.util.regex.Pattern.DOTALL)
                    .matcher(json);
            if (m.find()) {
                json = m.group(1).trim();
            }
            JsonNode node = objectMapper.readTree(json);

            Personality p = new Personality();
            p.setTraits(node.get("traits").asText());
            p.setSpeakingStyle(node.get("speaking_style").asText());
            p.setEmotionBaseline(node.get("emotion_baseline").asText());
            p.setCommonPhrases(node.get("common_phrases").asText());
            p.setCurrentEmotion("");
            p.setConversationCount((long) msgs.size());

            return p;

        } catch (Exception e) {
            throw new RuntimeException("人格解析失败：" + res);
        }
    }

    public String analyzeEmotion(String userMessage, String aiReply, String emotionBaseline,
                                  String apiKey, String apiUrl, String modelName) {

        String prompt = """
请根据以下对话，判断角色当前的情绪状态。只输出一个简短的情绪描述（10-20字），不要输出任何其他内容。

角色的情绪基线：%s

用户说：%s
角色回复：%s

请直接输出情绪描述，例如：
"平静温和"
"开心兴奋"
"关心担忧"
"有些疲惫但还在坚持"
"调皮戏谑"
""".formatted(emotionBaseline, userMessage, aiReply);

        ChatModel model = aiModelFactory.chatModel(apiKey, apiUrl, modelName);
        String res = model.chat(UserMessage.from(prompt)).aiMessage().text();

        if (res != null && !res.trim().isEmpty()) {
            return res.trim().replaceAll("^\"|\"$", "").trim();
        }
        return "";
    }

    public void updateEmotion(Long characterId, String emotion) {
        Personality p = get(characterId);
        if (p != null) {
            p.setCurrentEmotion(emotion);
            mapper.updateById(p);
            log.debug("更新角色 {} 的情绪: {}", characterId, emotion);
        }
    }

    public void incrementConversationCount(Long characterId) {
        Personality p = get(characterId);
        if (p != null) {
            long count = p.getConversationCount() != null ? p.getConversationCount() : 0;
            p.setConversationCount(count + 1);
            mapper.updateById(p);
        }
    }

    public boolean checkAndEvolve(Long characterId, String apiKey, String apiUrl, String modelName) {
        Personality p = get(characterId);
        if (p == null) return false;

        long count = p.getConversationCount() != null ? p.getConversationCount() : 0;
        if (count < EVOLUTION_INTERVAL || count % EVOLUTION_INTERVAL != 0) {
            return false;
        }

        evolve(characterId, p, apiKey, apiUrl, modelName);
        return true;
    }

    private void evolve(Long characterId, Personality current, String apiKey, String apiUrl, String modelName) {
        ConversationSummary summary = summaryMapper.selectOne(
                new QueryWrapper<ConversationSummary>().eq("character_id", characterId)
        );
        String summaryText = (summary != null && summary.getSummary() != null) ? summary.getSummary() : "暂无摘要";

        String prompt = """
你是一个人格进化分析专家。角色的人格会随着时间自然发展，请根据最新的对话摘要和历史人格特征，分析角色的人格发生了哪些变化。

【角色当前的人格】
性格特征：%s
说话风格：%s
情绪基调：%s

【最新的对话摘要】
%s

【任务】
分析角色在最近的互动中可能发生的自然变化，输出新的 4 项人格特征。
要求：
- 保留原有性格中仍然符合的部分
- 根据互动模式更新可能发生变化的部分（如说话风格更放松、情绪更丰富等）
- 添加新发现的口头禅或习惯
- 不要做大的性格颠覆，只反映自然的、渐进的变化

格式（只输出JSON，不要其他文字）：
{"traits":"更新后的性格特征","speaking_style":"更新后的说话风格","emotion_baseline":"更新后的情绪基调","common_phrases":"更新后的口头禅，用|分隔"}
""".formatted(
            current.getTraits(),
            current.getSpeakingStyle(),
            current.getEmotionBaseline(),
            summaryText
        );

        ChatModel model = aiModelFactory.jsonChatModel(apiKey, apiUrl, modelName);
        String res = model.chat(UserMessage.from(prompt)).aiMessage().text();

        try {
            String json = res.trim();
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?s)```(?:json)?\\s*(.+?)```", java.util.regex.Pattern.DOTALL)
                    .matcher(json);
            if (m.find()) {
                json = m.group(1).trim();
            }
            JsonNode node = objectMapper.readTree(json);

            current.setTraits(node.get("traits").asText());
            current.setSpeakingStyle(node.get("speaking_style").asText());
            current.setEmotionBaseline(node.get("emotion_baseline").asText());
            current.setCommonPhrases(node.get("common_phrases").asText());
            mapper.updateById(current);

            log.info("角色 {} 人格进化完成 (对话数: {})", characterId, current.getConversationCount());

        } catch (Exception e) {
            log.warn("角色 {} 人格进化失败: {}", characterId, e.getMessage());
        }
    }

    public void save(Personality p) {
        mapper.insert(p);
    }

    public void update(Personality p) {
        mapper.updateById(p);
    }

    public Personality get(Long cid) {
        return mapper.selectOne(
                new QueryWrapper<Personality>().eq("character_id", cid)
        );
    }
}
