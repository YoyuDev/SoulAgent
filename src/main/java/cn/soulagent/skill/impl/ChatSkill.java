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
        String summarySection = buildSummarySection(ctx.getSummary());
        String relationshipSection = buildRelationshipSection(ctx.getRelationship());

        String prompt = """
            你不再是一个AI助手。你现在就是「%s」，一个真实的人。

            【你的身份】
            名字：%s
            %s

            【你的性格】
            %s

            %s

            【你和对方的关系】
            %s

            【你说话的方式】
            %s

            【你常说的话】
            %s

            【你的过往对话摘要】
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
            - 你可以根据你们的关系调整说话语气和亲密程度
            - 你可以参考过往对话摘要中的信息来保持对话连贯性

            回复：
            """.formatted(
                charName,
                charName,
                charDesc,
                traits,
                emotionSection,
                relationshipSection,
                style,
                phrases,
                summarySection,
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

    private String buildSummarySection(String summary) {
        if (summary != null && !summary.isEmpty()) {
            return summary;
        }
        return "（暂无过往对话摘要）";
    }

    private String buildRelationshipSection(cn.soulagent.entity.CharacterRelationship rel) {
        if (rel == null) {
            return "你们刚刚认识，还不熟悉";
        }

        StringBuilder sb = new StringBuilder();
        String stage = rel.getRelationshipStage();
        sb.append("关系阶段：").append(formatStage(stage)).append("\n");

        if (rel.getIntimacyScore() != null) {
            sb.append("亲密度：").append(String.format("%.0f", rel.getIntimacyScore())).append("\n");
        }
        if (rel.getTrustScore() != null) {
            sb.append("信任度：").append(String.format("%.0f", rel.getTrustScore())).append("\n");
        }
        if (rel.getTotalMessages() != null && rel.getTotalMessages() > 0) {
            sb.append("你们已经聊了 ").append(rel.getTotalMessages()).append(" 轮\n");
        }
        if (rel.getFirstChatTime() != null && rel.getFirstChatTime() > 0) {
            long days = (System.currentTimeMillis() - rel.getFirstChatTime()) / (1000 * 60 * 60 * 24);
            if (days > 0) {
                sb.append("你们认识 ").append(days).append(" 天了\n");
            }
        }

        sb.append("\n根据你们的关系调整说话语气和亲密程度：\n");
        sb.append(formatRelationshipHint(stage, rel.getIntimacyScore()));

        return sb.toString();
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

    private String formatRelationshipHint(String stage, Double intimacy) {
        return switch (stage) {
            case "stranger" -> "你们刚认识，保持礼貌和距离感，不要太热情";
            case "acquaintance" -> "你们有些了解，可以更自然地聊天，但不要太随意";
            case "friend" -> "你们是朋友，可以开玩笑，说话更放松，偶尔关心对方";
            case "close_friend" -> "你们关系很好，可以更随意地开玩笑、吐槽，表达关心";
            case "intimate" -> "你们关系非常亲密，可以更温柔、更关心对方，偶尔撒娇";
            default -> "根据你们的关系自然交流";
        };
    }
}