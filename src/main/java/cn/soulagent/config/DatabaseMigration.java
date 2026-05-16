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
        // 创建主表
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS character (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    avatar TEXT DEFAULT '',
                    random_event_enabled INTEGER NOT NULL DEFAULT 1,
                    last_event_time INTEGER NOT NULL DEFAULT 0
                )
                """);
            log.info("数据库迁移：character 表已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：character 表已存在，跳过");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_character_name ON character(name)");
            log.info("数据库迁移：索引 idx_character_name 已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：索引 idx_character_name 已存在，跳过");
        }

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS personality (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    character_id INTEGER NOT NULL UNIQUE,
                    traits TEXT NOT NULL,
                    speaking_style TEXT NOT NULL,
                    emotion_baseline TEXT NOT NULL DEFAULT '平和',
                    common_phrases TEXT DEFAULT '',
                    current_emotion TEXT NOT NULL DEFAULT '',
                    conversation_count INTEGER NOT NULL DEFAULT 0
                )
                """);
            log.info("数据库迁移：personality 表已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：personality 表已存在，跳过");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_personality_character ON personality(character_id)");
            log.info("数据库迁移：索引 idx_personality_character 已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：索引 idx_personality_character 已存在，跳过");
        }

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_message (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    character_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    create_time INTEGER NOT NULL
                )
                """);
            log.info("数据库迁移：chat_message 表已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：chat_message 表已存在，跳过");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_message_character ON chat_message(character_id)");
            log.info("数据库迁移：索引 idx_message_character 已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：索引 idx_message_character 已存在，跳过");
        }

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_setting (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    setting_key TEXT NOT NULL UNIQUE,
                    setting_value TEXT DEFAULT ''
                )
                """);
            log.info("数据库迁移：app_setting 表已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：app_setting 表已存在，跳过");
        }

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS conversation_summary (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    character_id INTEGER NOT NULL UNIQUE,
                    summary TEXT NOT NULL DEFAULT '',
                    last_updated INTEGER NOT NULL,
                    message_count INTEGER NOT NULL DEFAULT 0
                )
                """);
            log.info("数据库迁移: conversation_summary 表已创建");
        } catch (Exception e) {
            log.debug("数据库迁移: conversation_summary 表已存在，跳过");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_summary_character ON conversation_summary(character_id)");
            log.info("数据库迁移: 索引 idx_summary_character 已创建");
        } catch (Exception e) {
            log.debug("数据库迁移: 索引 idx_summary_character 已存在，跳过");
        }

        try {
            jdbcTemplate.execute("""
                ALTER TABLE personality ADD COLUMN conversation_count INTEGER NOT NULL DEFAULT 0
                """);
            log.info("数据库迁移: personality.conversation_count 已添加");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("duplicate column")) {
                log.debug("数据库迁移: personality.conversation_count 已存在，跳过");
            } else {
                log.debug("数据库迁移: personality.conversation_count 可能已存在: {}", e.getMessage());
            }
        }

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS character_relationship (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    character_id INTEGER NOT NULL UNIQUE,
                    intimacy_score REAL NOT NULL DEFAULT 0.0,
                    trust_score REAL NOT NULL DEFAULT 0.0,
                    first_chat_time INTEGER NOT NULL DEFAULT 0,
                    last_chat_time INTEGER NOT NULL DEFAULT 0,
                    total_messages INTEGER NOT NULL DEFAULT 0,
                    relationship_stage TEXT NOT NULL DEFAULT 'stranger'
                )
                """);
            log.info("数据库迁移: character_relationship 表已创建");
        } catch (Exception e) {
            log.debug("数据库迁移: character_relationship 表已存在，跳过");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_relationship_character ON character_relationship(character_id)");
            log.info("数据库迁移：索引 idx_relationship_character 已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：索引 idx_relationship_character 已存在，跳过");
        }

        try {
            jdbcTemplate.execute("""
                ALTER TABLE soul_character ADD COLUMN random_event_enabled INTEGER NOT NULL DEFAULT 1
                """);
            log.info("数据库迁移：soul_character.random_event_enabled 已添加");
        } catch (Exception e) {
            log.debug("数据库迁移：soul_character.random_event_enabled 可能已存在：{}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                ALTER TABLE soul_character ADD COLUMN last_event_time INTEGER NOT NULL DEFAULT 0
                """);
            log.info("数据库迁移：soul_character.last_event_time 已添加");
        } catch (Exception e) {
            log.debug("数据库迁移：soul_character.last_event_time 可能已存在：{}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS random_event (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    character_id INTEGER NOT NULL,
                    event_type TEXT NOT NULL,
                    event_content TEXT NOT NULL,
                    event_time INTEGER NOT NULL,
                    is_shared INTEGER NOT NULL DEFAULT 0,
                    share_time INTEGER DEFAULT 0
                )
                """);
            log.info("数据库迁移：random_event 表已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：random_event 表已存在，跳过");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_event_character ON random_event(character_id)");
            log.info("数据库迁移：索引 idx_event_character 已创建");
        } catch (Exception e) {
            log.debug("数据库迁移：索引 idx_event_character 已存在，跳过");
        }
    }
}