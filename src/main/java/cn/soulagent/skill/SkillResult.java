package cn.soulagent.skill;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SkillResult {

    private final String prompt;

    private final boolean directReply;

    private final String directContent;

    public static SkillResult prompt(String prompt) {
        return SkillResult.builder().prompt(prompt).directReply(false).build();
    }

    public static SkillResult reply(String content) {
        return SkillResult.builder().directContent(content).directReply(true).build();
    }

    public boolean shouldCallLlm() {
        return !directReply && prompt != null && !prompt.isEmpty();
    }
}