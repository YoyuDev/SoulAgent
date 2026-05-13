package cn.soulagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseMigration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        try {
            jdbcTemplate.execute("""
                ALTER TABLE personality ADD COLUMN current_emotion TEXT NOT NULL DEFAULT ''
                """);
            log.info("数据库迁移: personality.current_emotion 已添加");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("duplicate column")) {
                log.debug("数据库迁移: personality.current_emotion 已存在，跳过");
            } else {
                log.warn("数据库迁移异常（可能已执行过）: {}", e.getMessage());
            }
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_personality_character ON personality(character_id)");
            log.info("数据库迁移: 索引 idx_personality_character 已创建");
        } catch (Exception e) {
            log.debug("数据库迁移: 索引 idx_personality_character 已存在，跳过");
        }
    }
}