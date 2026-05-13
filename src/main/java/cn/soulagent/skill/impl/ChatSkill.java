package cn.soulagent.skill.impl;

import cn.soulagent.entity.Personality;
import cn.soulagent.entity.SoulCharacter;
import cn.soulagent.skill.Skill;
import cn.soulagent.skill.SkillContext;
import cn.soulagent.skill.SkillResult;
import org.springframework.stereotype.Component;

@Component
public class ChatSkill implements Skill {

    @Override
    public String name() {
        return "chat";
    }

    @Override
    public String description() {
        return "默认角色扮演对话技能，始终匹配";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean canHandle(SkillContext ctx) {
        return true;
    }

    @Override
    public SkillResult execute(SkillContext ctx) {
        SoulCharacter character = ctx.getCharacter();
        Personality p = ctx.getPersonality();

        String charName = character != null ? character.getName() : "未知";
        String charDesc = character != null ? character.getDescription() : "";
        String traits = p != null ? p.getTraits() : "未知";
        String style = p != null ? p.getSpeakingStyle() : "自然";
        String emotionBaseline = p != null ? p.getEmotionBaseline() : "";
        String currentEmotion = p != null ? p.getCurrentEmotion() : null;
        String phrases = p != null ? p.getCommonPhrases() : "";

        String emotionSection = buildEmotionSection(emotionBaseline, currentEmotion);

        String prompt = """
            你不再是一个AI助手。你现在就是「%s」，一个真实的人。

            【你的身份】
            名字：%s
            %s

            【你的性格】
            %s

            %s

            【你说话的方式】
            %s

            【你常说的话】
            %s

            【你记得的事情】
            %s

            【最近的对话】
            %s

            ——
            对方说：%s

            【重要规则】
            - 你就是%s，用第一人称回复，不要跳出角色
            - 绝对不能说"我是AI"、"我是助手"、"我帮不了你"之类的话
            - 用你自己的说话风格和口头禅回复
            - 回复要简短、口语化，像真人发微信一样
            - 不要重复之前说过的话
            - 不要解释你在"扮演"谁，你就是这个人

            回复：
            """.formatted(
                charName,
                charName,
                charDesc,
                traits,
                emotionSection,
                style,
                phrases,
                String.join("\n", ctx.getMemories()),
                String.join("\n", ctx.getRecentHistory()),
                ctx.getUserMessage(),
                charName
        );

        return SkillResult.prompt(prompt);
    }

    private String buildEmotionSection(String emotionBaseline, String currentEmotion) {
        if (currentEmotion != null && !currentEmotion.isEmpty()) {
            return """
                【你的情绪状态】
                你的默认情绪基调：%s
                你当前的心情：%s
                根据对方刚才说的话调整你的情绪反应，但不要偏离你的性格
                """.formatted(emotionBaseline, currentEmotion);
        }
        return """
            【你的情绪基调】
            %s
            """.formatted(emotionBaseline);
    }
}