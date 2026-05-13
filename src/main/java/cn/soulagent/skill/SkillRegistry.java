package cn.soulagent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(SkillRegistry.class);

    private final List<Skill> skills;

    public SkillRegistry(List<Skill> skills) {
        this.skills = new ArrayList<>(skills);
        this.skills.sort(Comparator.comparingInt(Skill::priority).reversed());
        log.info("SkillRegistry 已加载 {} 个技能: {}", skills.size(),
                skills.stream().map(s -> s.name() + "(" + s.priority() + ")").toList());
    }

    public List<Skill> listAll() {
        return List.copyOf(skills);
    }

    public Skill findMatching(SkillContext ctx) {
        for (Skill skill : skills) {
            if (skill.canHandle(ctx)) {
                log.debug("技能匹配: {} -> {}", ctx.getUserMessage(), skill.name());
                return skill;
            }
        }
        return null;
    }
}