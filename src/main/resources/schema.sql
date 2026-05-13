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
    character_id INTEGER NOT NULL,
    traits TEXT NOT NULL DEFAULT '',
    speaking_style TEXT NOT NULL DEFAULT '',
    emotion_baseline TEXT NOT NULL DEFAULT '',
    common_phrases TEXT NOT NULL DEFAULT '',
    current_emotion TEXT NOT NULL DEFAULT ''
);

CREATE INDEX IF NOT EXISTS idx_personality_character ON personality(character_id);

CREATE TABLE IF NOT EXISTS app_setting (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    setting_key TEXT NOT NULL UNIQUE,
    setting_value TEXT NOT NULL
);
