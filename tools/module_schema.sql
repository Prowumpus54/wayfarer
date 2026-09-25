PRAGMA foreign_keys=ON;

CREATE TABLE metadata (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE locations (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    area TEXT,
    parent_id TEXT,
    kind TEXT,
    player_description TEXT,
    gm_notes TEXT,
    map_asset TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE connections (
    from_id TEXT NOT NULL,
    to_id TEXT NOT NULL,
    travel_text TEXT,
    locked INTEGER NOT NULL DEFAULT 0,
    requirements_json TEXT NOT NULL DEFAULT '[]',
    PRIMARY KEY (from_id, to_id)
);

CREATE TABLE npcs (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    role TEXT,
    location_id TEXT,
    description TEXT,
    gm_notes TEXT,
    creature_rule_ref TEXT,
    disposition TEXT
);

CREATE TABLE encounters (
    id TEXT PRIMARY KEY,
    location_id TEXT NOT NULL,
    name TEXT NOT NULL,
    difficulty TEXT,
    trigger_text TEXT,
    gm_notes TEXT
);

CREATE TABLE encounter_creatures (
    encounter_id TEXT NOT NULL,
    creature_rule_ref TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    role TEXT,
    overrides_json TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE treasure (
    id TEXT PRIMARY KEY,
    location_id TEXT,
    name TEXT NOT NULL,
    rule_ref TEXT,
    quantity INTEGER NOT NULL DEFAULT 1,
    hidden INTEGER NOT NULL DEFAULT 0,
    requirements_json TEXT NOT NULL DEFAULT '[]',
    gm_notes TEXT
);

CREATE TABLE quests (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    gm_notes TEXT
);

CREATE TABLE quest_steps (
    id TEXT PRIMARY KEY,
    quest_id TEXT NOT NULL,
    sequence INTEGER NOT NULL,
    description TEXT NOT NULL,
    trigger_id TEXT,
    completion_flag TEXT
);

CREATE TABLE triggers (
    id TEXT PRIMARY KEY,
    location_id TEXT,
    event TEXT NOT NULL,
    condition_json TEXT NOT NULL DEFAULT '{}',
    effects_json TEXT NOT NULL DEFAULT '[]',
    gm_notes TEXT
);

CREATE TABLE handouts (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    body TEXT,
    asset_path TEXT,
    gm_only INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_locations_area ON locations(area, sort_order);
CREATE INDEX idx_npcs_location ON npcs(location_id);
CREATE INDEX idx_encounters_location ON encounters(location_id);
CREATE INDEX idx_treasure_location ON treasure(location_id);
CREATE INDEX idx_triggers_location ON triggers(location_id, event);
