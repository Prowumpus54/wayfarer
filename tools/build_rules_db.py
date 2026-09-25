from pathlib import Path
import html
import json
import re
import sqlite3
import time

ROOT = Path(r"C:\Wayfarer\data-source\pf2e\packs\pf2e")
OUT = Path(r"C:\Wayfarer\app\src\main\assets\wayfarer_rules.sqlite")

SOURCES = {
    "class": ["classes"],
    "class_feature": ["class-features"],
    "ancestry": ["ancestries"],
    "ancestry_feature": ["ancestry-features"],
    "heritage": ["heritages"],
    "background": ["backgrounds"],
    "feat": ["feats"],
    "spell": ["spells"],
    "action": ["actions"],
    "condition": ["conditions"],
    "equipment": ["equipment"],
    "equipment_effect": ["equipment-effects"],
    "feat_effect": ["feat-effects"],
    "spell_effect": ["spell-effects"],
    "deity": ["deities"],
    "familiar_ability": ["familiar-abilities"],
    "hazard": ["hazards"],
    "creature": [
        "pathfinder-monster-core",
        "pathfinder-monster-core-2",
        "pathfinder-bestiary",
        "pathfinder-bestiary-2",
        "pathfinder-bestiary-3",
    ],
}

TAG_RE = re.compile(r"<[^>]+>")
BR_RE = re.compile(r"<br\s*/?>|</p>|</div>|</li>", re.I)
UUID_RE = re.compile(r"@UUID\[[^\]]+\](?:\{([^}]+)\})?")


def compact(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def plain_text(value):
    if not value:
        return ""
    value = UUID_RE.sub(lambda m: m.group(1) or "", str(value))
    value = BR_RE.sub("\n", value)
    value = TAG_RE.sub("", value)
    value = html.unescape(value)
    return re.sub(r"\n{3,}", "\n\n", value).strip()


def nested_int(value, default=0):
    if isinstance(value, dict):
        value = value.get("value", default)
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def list_value(value):
    if isinstance(value, dict):
        value = value.get("value", [])
    return value if isinstance(value, list) else []


def iter_json(folder):
    for path in folder.rglob("*.json"):
        if path.name == "_folders.json":
            continue
        yield path


def make_schema(db):
    db.executescript("""
    PRAGMA journal_mode=OFF;
    PRAGMA synchronous=OFF;
    PRAGMA temp_store=MEMORY;
    CREATE TABLE metadata (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
    );
    CREATE TABLE entries (
        uid TEXT PRIMARY KEY,
        source_id TEXT,
        kind TEXT NOT NULL,
        name TEXT NOT NULL,
        name_search TEXT NOT NULL,
        level INTEGER NOT NULL DEFAULT 0,
        category TEXT,
        subtype TEXT,
        rarity TEXT,
        traits_json TEXT NOT NULL,
        traditions_json TEXT NOT NULL,
        prerequisites_json TEXT NOT NULL,
        actions INTEGER,
        action_type TEXT,
        description TEXT,
        license TEXT,
        remaster INTEGER NOT NULL DEFAULT 0,
        source_title TEXT,
        source_group TEXT,
        source_path TEXT NOT NULL,
        raw_json TEXT NOT NULL
    );
    CREATE INDEX idx_entries_kind_name ON entries(kind, name_search);
    CREATE INDEX idx_entries_kind_level ON entries(kind, level);
    CREATE INDEX idx_entries_category ON entries(category);
    CREATE INDEX idx_entries_remaster ON entries(remaster);
    CREATE TABLE classes (
        uid TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        hp INTEGER NOT NULL,
        key_abilities_json TEXT NOT NULL,
        perception INTEGER NOT NULL,
        fortitude INTEGER NOT NULL,
        reflex INTEGER NOT NULL,
        will INTEGER NOT NULL,
        attacks_json TEXT NOT NULL,
        defenses_json TEXT NOT NULL,
        class_feat_levels_json TEXT NOT NULL,
        skill_feat_levels_json TEXT NOT NULL,
        general_feat_levels_json TEXT NOT NULL,
        ancestry_feat_levels_json TEXT NOT NULL,
        skill_increase_levels_json TEXT NOT NULL,
        features_json TEXT NOT NULL,
        license TEXT,
        remaster INTEGER NOT NULL,
        source_title TEXT,
        description TEXT
    );
    CREATE VIEW feats AS SELECT * FROM entries WHERE kind='feat';
    CREATE VIEW spells AS SELECT * FROM entries WHERE kind='spell';
    CREATE VIEW equipment AS SELECT * FROM entries WHERE kind='equipment';
    CREATE VIEW creatures AS SELECT * FROM entries WHERE kind='creature';
    CREATE VIEW actions AS SELECT * FROM entries WHERE kind='action';
    CREATE VIEW conditions AS SELECT * FROM entries WHERE kind='condition';
    """)


def record_for(kind, base, path, obj):
    system = obj.get("system") or {}
    traits = system.get("traits") or {}
    publication = system.get("publication") or {}
    rel = path.relative_to(ROOT).as_posix()
    group = path.relative_to(base).parent.as_posix()
    level = nested_int(system.get("level"), 0)
    rarity = traits.get("rarity") if isinstance(traits, dict) else None
    trait_values = list_value(traits)
    traditions = traits.get("traditions", []) if isinstance(traits, dict) else []
    prereqs = list_value(system.get("prerequisites"))
    description = plain_text((system.get("description") or {}).get("value", ""))
    actions = system.get("actions")
    if isinstance(actions, dict):
        actions = actions.get("value")
    try:
        actions = int(actions) if actions not in (None, "") else None
    except (TypeError, ValueError):
        actions = None
    return (
        kind + ":" + rel,
        obj.get("_id"),
        kind,
        obj.get("name") or path.stem.replace("-", " ").title(),
        (obj.get("name") or path.stem).casefold(),
        level,
        system.get("category"),
        obj.get("type"),
        rarity,
        compact(trait_values),
        compact(traditions),
        compact(prereqs),
        actions,
        system.get("actionType", {}).get("value") if isinstance(system.get("actionType"), dict) else system.get("actionType"),
        description,
        publication.get("license"),
        1 if publication.get("remaster") else 0,
        publication.get("title"),
        group,
        rel,
        compact(obj),
    )


ENTRY_SQL = """
INSERT OR REPLACE INTO entries (
    uid, source_id, kind, name, name_search, level, category, subtype,
    rarity, traits_json, traditions_json, prerequisites_json, actions,
    action_type, description, license, remaster, source_title,
    source_group, source_path, raw_json
) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
"""


def class_record(uid, obj):
    system = obj.get("system") or {}
    pub = system.get("publication") or {}
    saves = system.get("savingThrows") or {}
    items = system.get("items") or {}
    features = sorted(
        [{"name": v.get("name"), "level": v.get("level"), "uuid": v.get("uuid")} for v in items.values()],
        key=lambda x: (x.get("level") or 0, x.get("name") or ""),
    )
    description = plain_text((system.get("description") or {}).get("value", ""))
    return (
        uid,
        obj.get("name") or "",
        int(system.get("hp") or 0),
        compact(list_value(system.get("keyAbility"))),
        int(system.get("perception") or 0),
        int(saves.get("fortitude") or 0),
        int(saves.get("reflex") or 0),
        int(saves.get("will") or 0),
        compact(system.get("attacks") or {}),
        compact(system.get("defenses") or {}),
        compact(list_value(system.get("classFeatLevels"))),
        compact(list_value(system.get("skillFeatLevels"))),
        compact(list_value(system.get("generalFeatLevels"))),
        compact(list_value(system.get("ancestryFeatLevels"))),
        compact(list_value(system.get("skillIncreaseLevels"))),
        compact(features),
        pub.get("license"),
        1 if pub.get("remaster") else 0,
        pub.get("title"),
        description,
    )


CLASS_SQL = """
INSERT OR REPLACE INTO classes VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
"""


def build():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    if OUT.exists():
        OUT.unlink()
    db = sqlite3.connect(OUT)
    make_schema(db)
    counts = {}
    started = time.time()

    for kind, folders in SOURCES.items():
        count = 0
        for folder_name in folders:
            base = ROOT / folder_name
            if not base.exists():
                continue
            batch = []
            class_batch = []
            for path in iter_json(base):
                try:
                    obj = json.loads(path.read_text(encoding="utf-8"))
                except Exception as exc:
                    print("SKIP", path, exc)
                    continue
                row = record_for(kind, base, path, obj)
                batch.append(row)
                if kind == "class":
                    class_batch.append(class_record(row[0], obj))
                count += 1
                if len(batch) >= 500:
                    db.executemany(ENTRY_SQL, batch)
                    batch.clear()
                if len(class_batch) >= 100:
                    db.executemany(CLASS_SQL, class_batch)
                    class_batch.clear()
            if batch:
                db.executemany(ENTRY_SQL, batch)
            if class_batch:
                db.executemany(CLASS_SQL, class_batch)
            db.commit()
        counts[kind] = count
        print(f"{kind}: {count}")

    db.executemany(
        "INSERT INTO metadata(key,value) VALUES (?,?)",
        [
            ("schema_version", "1"),
            ("generated_utc", time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())),
            ("source", "foundryvtt/pf2e"),
            ("counts_json", compact(counts)),
        ],
    )
    db.execute("ANALYZE")
    db.commit()
    db.execute("VACUUM")
    db.commit()
    total = db.execute("SELECT COUNT(*) FROM entries").fetchone()[0]
    remaster = db.execute("SELECT COUNT(*) FROM entries WHERE remaster=1").fetchone()[0]
    db.close()
    size_mb = OUT.stat().st_size / (1024 * 1024)
    print(f"TOTAL: {total}")
    print(f"REMASTER: {remaster}")
    print(f"SIZE_MB: {size_mb:.1f}")
    print(f"SECONDS: {time.time() - started:.1f}")
    print(f"OUTPUT: {OUT}")


if __name__ == "__main__":
    build()
