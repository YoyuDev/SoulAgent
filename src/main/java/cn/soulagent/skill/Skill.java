package cn.soulagent.skill;

public interface Skill {

    String name();

    String description();

    int priority();

    boolean canHandle(SkillContext ctx);

    SkillResult execute(SkillContext ctx);
}