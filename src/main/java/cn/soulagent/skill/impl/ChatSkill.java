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
        String emotion = p != null ? p.getEmotionBaseline() : "";
        String phrases = p != null ? p.getCommonPhrases() : "";

        String prompt = """
            你不再是一个AI助手。你现在就是「%s」，一个真实的人。

            【你的身份】
            名字：%s
            %s

            【你的性格】
            %s

            【你的情绪基调】
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
                emotion,
                style,
                phrases,
                String.join("\n", ctx.getMemories()),
                String.join("\n", ctx.getRecentHistory()),
                ctx.getUserMessage(),
                charName
        );

        return SkillResult.prompt(prompt);
    }
}