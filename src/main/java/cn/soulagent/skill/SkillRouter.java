package cn.soulagent.skill;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SkillRouter {

    private static final Logger log = LoggerFactory.getLogger(SkillRouter.class);

    private final SkillRegistry registry;

    public SkillResult route(SkillContext ctx) {
        Skill skill = registry.findMatching(ctx);

        if (skill == null) {
            log.warn("没有技能匹配消息: {}", ctx.getUserMessage());
            return SkillResult.reply("抱歉，我不知道该怎么回应。");
        }

        log.info("路由到技能: {} (消息: {})", skill.name(),
                ctx.getUserMessage().substring(0, Math.min(30, ctx.getUserMessage().length())));

        return skill.execute(ctx);
    }
}