package cn.soulagent.service;

import cn.soulagent.entity.Personality;
import cn.soulagent.mapper.PersonalityMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalityService {

    private final PersonalityMapper mapper;
    private final AiModelFactory aiModelFactory;
    private final ObjectMapper objectMapper;

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
            // 结构化输出模式，AI 直接返回合法 JSON，但仍做基本容错
            String json = res.trim();
            // 去掉可能的 markdown 包裹
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

            return p;

        } catch (Exception e) {
            throw new RuntimeException("人格解析失败：" + res);
        }
    }

    public void save(Personality p) {
        mapper.insert(p);
    }

    public Personality get(Long cid) {
        return mapper.selectOne(
                new QueryWrapper<Personality>().eq("character_id", cid)
        );
    }
}
