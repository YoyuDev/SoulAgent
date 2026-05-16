CREATE TABLE IF NOT EXISTS character (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    avatar TEXT DEFAULT '',
    random_event_enabled INTEGER NOT NULL DEFAULT 1,
    last_event_time INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_character_name ON character(name);

CREATE TABLE IF NOT EXISTS chat_message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    create_time INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_message_character ON chat_message(character_id, create_time);

CREATE TABLE IF NOT EXISTS personality (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL UNIQUE,
    traits TEXT NOT NULL DEFAULT '',
    speaking_style TEXT NOT NULL DEFAULT '',
    emotion_baseline TEXT NOT NULL DEFAULT '',
    common_phrases TEXT NOT NULL DEFAULT '',
    current_emotion TEXT NOT NULL DEFAULT '',
    conversation_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_personality_character ON personality(character_id);

CREATE TABLE IF NOT EXISTS app_setting (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    setting_key TEXT NOT NULL UNIQUE,
    setting_value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS conversation_summary (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL UNIQUE,
    summary TEXT NOT NULL DEFAULT '',
    last_updated INTEGER NOT NULL,
    message_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_summary_character ON conversation_summary(character_id);

CREATE TABLE IF NOT EXISTS character_relationship (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL UNIQUE,
    intimacy_score REAL NOT NULL DEFAULT 0.0,
    trust_score REAL NOT NULL DEFAULT 0.0,
    first_chat_time INTEGER NOT NULL DEFAULT 0,
    last_chat_time INTEGER NOT NULL DEFAULT 0,
    total_messages INTEGER NOT NULL DEFAULT 0,
    relationship_stage TEXT NOT NULL DEFAULT 'stranger'
);

CREATE INDEX IF NOT EXISTS idx_relationship_character ON character_relationship(character_id);

CREATE TABLE IF NOT EXISTS random_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    event_content TEXT NOT NULL,
    event_time INTEGER NOT NULL,
    is_shared INTEGER NOT NULL DEFAULT 0,
    share_time INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_event_character ON random_event(character_id);
