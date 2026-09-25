from pathlib import Path
import json
import re
import sqlite3

ROOT = Path(r"C:\Wayfarer")
SOURCE = ROOT / "modules/source/SunlessCitadel/sunless_ocr.txt"
SCHEMA = ROOT / "tools/module_schema.sql"
OUT_DIR = ROOT / "modules/built/sunless_citadel"
DB_PATH = OUT_DIR / "module.sqlite"

ROOMS = [
    (0, "Dark Ravine"),
    (1, "Ledge"),
    (2, "Switchback Stairs"),
    (3, "Crumbled Courtyard"),
    (4, "Tower Shell"),
    (5, "Secret Pocket"),
    (6, "Old Approach"),
    (7, "Gallery of Forlorn Notes"),
    (8, "Pressure Plate"),
    (9, "The Dragon's Riddle"),
    (10, "Honor Guard"),
    (11, "Secret Room"),
    (12, "Tomb of a Failed Dragonpriest"),
    (13, "Empty"),
    (14, "Enchanted Water Cache"),
    (15, "Flew the Coop"),
    (16, "Kobold Bounders"),
    (17, "Dragon Chow"),
    (18, "Prisoners of War"),
    (19, "Hall of Dragons"),
    (20, "Kobold Colony"),
    (21, "The Dragon Throne"),
    (22, "Larder"),
    (23, "Underdark Access"),
    (24, "Trapped Access"),
    (25, "Desolate"),
    (26, "Dry Fountain"),
    (27, "Sanctuary"),
    (28, "Infested Cells"),
    (29, "Old Traps"),
    (30, "Mama Rat"),
    (31, "Caltrop Hall"),
    (32, "Goblin Gate"),
    (33, "Practice Range"),
    (34, "Goblin Stockade"),
    (35, "Trapped Corridor"),
    (36, "Goblin Bandits"),
    (37, "Trophy Room"),
    (38, "Goblin Through-Way"),
    (39, "Dragon Haze"),
    (40, "Goblinville"),
    (41, "Hall of the Goblin Chief"),
    (42, "Compost Central"),
    (43, "The Great Hunter"),
    (44, "Rift"),
    (45, "Rift Node"),
    (46, "Old Shrine"),
    (47, "Belak's Goblin Common"),
    (48, "Gallery"),
    (49, "Arboretum"),
    (50, "Ashardalon's Shrine"),
    (51, "Dragon Library"),
    (52, "Underpass"),
    (53, "Nature's Lore"),
    (54, "Grove Gate"),
    (55, "Twilight Grove"),
    (56, "The Gulthias Tree"),
]

ALIASES = {
    0: ["O. Dark Ravine", "0. Dark Ravine"],
    1: ["Ledge"],
    2: ["2. Switchback Stairs"],
    11: ["ll. Secret Room", "11. Secret Room"],
    18: ["18, Prisoners of War", "18. Prisoners of War"],
    39: ["39, Dragon Haze", "39. Dragon Haze"],
    45: ["4S. Rift Node", "45. Rift Node"],
}

KEY_NPCS = [
    "Kerowyn Hucrele", "Talgen Hucrele", "Sharwyn Hucrele",
    "Sir Braford", "Karakas", "Meepo", "Yusdrayl", "Calcryx",
    "Erky Timbers", "Durnn", "Grenl", "Balsag", "Belak",
]

NPC_LOCATIONS = {
    "Kerowyn Hucrele": "oakhurst",
    "Talgen Hucrele": "area_41",
    "Sharwyn Hucrele": "area_56",
    "Sir Braford": "area_56",
    "Karakas": "area_30",
    "Meepo": "area_15",
    "Yusdrayl": "area_21",
    "Calcryx": "area_37",
    "Erky Timbers": "area_34",
    "Durnn": "area_41",
    "Grenl": "area_41",
    "Balsag": "area_43",
    "Belak": "area_56",
}

def normalize(text: str) -> str:
    return (text.replace("�", "'")
        .replace("’", "'")
        .replace("‘", "'")
        .replace("“", '"')
        .replace("”", '"'))

def clean_text(text: str) -> str:
    text = normalize(text)
    text = re.sub(r"-\n(?=[a-z])", "", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()

def find_heading(lines, number, name, after):
    aliases = ALIASES.get(number, [])
    patterns = aliases + [f"{number}. {name}", f"{number}, {name}"]
    for i in range(after, len(lines)):
        candidate = normalize(lines[i]).strip()
        if number == 1 and candidate == "Ledge":
            return i
        if any(p.lower() in candidate.lower() for p in patterns):
            return i
    raise RuntimeError(f"Heading not found: {number} {name}")

def player_description(chunk: str, heading: str) -> str:
    body = chunk.split("\n", 1)[1] if "\n" in chunk else chunk
    paragraphs = re.split(r"\n\s*\n", body)
    bad = (
        "Creature", "Creatures", "Trap", "Tactics", "Treasure",
        "Development", "Statistics", "Overview", "Sidebar",
    )
    for para in paragraphs:
        p = re.sub(r"\s+", " ", clean_text(para)).strip()
        if len(p) < 60:
            continue
        if p.startswith(bad):
            continue
        if "hp " in p[:80].lower():
            continue
        if "shown on" in p.lower() and len(p) < 180:
            continue
        return p[:900]
    return f"Area {heading}."

def extract_section(chunk: str, label: str, max_len=1600):
    pattern = re.compile(
        rf"(?ims)^\s*{re.escape(label)}\s*:\s*(.+?)"
        rf"(?=\n\s*(?:Creature|Creatures|Trap|Tactics|Treasure|"
        rf"Development|Ad Hoc|Conclusion)\s*:|\Z)"
    )
    match = pattern.search(chunk)
    return clean_text(match.group(1))[:max_len] if match else ""

def encounter_sections(chunk: str):
    marker = re.compile(
        r"(?im)^\s*(Creature|Creatures|Trap)\s*\(EL\s*([^\)]+)\)\s*:"
    )
    matches = list(marker.finditer(chunk))
    out = []
    for idx, match in enumerate(matches):
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(chunk)
        section = clean_text(chunk[match.start():end])
        section = re.split(
            r"(?im)^\s*(?:Tactics|Treasure|Development)\s*:",
            section
        )[0]
        out.append((match.group(1), match.group(2), section[:2200]))
    return out

def connection_mentions(chunk: str):
    # Physical navigation usually appears before combat tactics/development.
    physical = re.split(
        r"(?im)^\s*(?:Creature|Creatures|Tactics|Treasure|Development)\s*(?:\([^\)]*\))?\s*:",
        chunk,
        maxsplit=1,
    )[0]
    mentions = set()
    keywords = (
        "lead", "door", "access", "connect", "opens", "stairs",
        "passage", "corridor", "archway", "threshold", "crawl",
        "entrance", "exit", "shaft", "tunnel", "begins", "far side",
    )
    reject = (
        "lies in area", "stationed in area", "keyed to area",
        "rooms keyed to area", "retreat to area",
    )
    for match in re.finditer(r"(?i)\barea\s+(\d{1,2})(?:[a-f])?\b", physical):
        number = int(match.group(1))
        if not 0 <= number <= 56:
            continue
        window = physical[max(0, match.start() - 110):match.end() + 110].lower()
        if any(phrase in window for phrase in reject):
            continue
        if any(word in window for word in keywords):
            mentions.add(number)
    return mentions

def best_area_for_name(chunks, name):
    needle = name.lower()
    scored = []
    for number, chunk in chunks.items():
        count = normalize(chunk).lower().count(needle)
        if count:
            scored.append((count, number))
    if not scored:
        return None
    scored.sort(reverse=True)
    return scored[0][1]

def make_manifest():
    manifest = {
        "id": "sunless_citadel",
        "title": "The Sunless Citadel",
        "version": "0.2.0",
        "ruleset": "pf2e-adapted",
        "description": (
            "A lost fortress beneath the earth, rival tribes in its halls, "
            "and a dark mystery rooted in the Twilight Grove."
        ),
        "startingLocation": "oakhurst",
        "minLevel": 1,
        "maxLevel": 3,
        "source": "The_Sunless_Citadel_3e.pdf",
    }
    (OUT_DIR / "manifest.json").write_text(
        json.dumps(manifest, indent=2),
        encoding="utf-8"
    )

def build():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    raw = SOURCE.read_text(encoding="utf-8", errors="ignore")
    lines = raw.splitlines()
    starts = {}
    cursor = 500
    for number, name in ROOMS:
        index = find_heading(lines, number, name, cursor)
        starts[number] = index
        cursor = index + 1

    chunks = {}
    for pos, (number, name) in enumerate(ROOMS):
        start = starts[number]
        if pos + 1 < len(ROOMS):
            end = starts[ROOMS[pos + 1][0]]
        else:
            end = min(len(lines), start + 550)
        chunks[number] = clean_text("\n".join(lines[start:end]))

    if DB_PATH.exists():
        DB_PATH.unlink()
    db = sqlite3.connect(DB_PATH)
    db.executescript(SCHEMA.read_text(encoding="utf-8"))

    metadata = [
        ("module_id", "sunless_citadel"),
        ("title", "The Sunless Citadel"),
        ("module_version", "0.2.0"),
        ("source_file", "The_Sunless_Citadel_3e.pdf"),
        ("ruleset", "pf2e-adapted"),
        ("source_ocr", "tesseract-5.4 single-pass"),
    ]
    db.executemany("INSERT INTO metadata(key,value) VALUES (?,?)", metadata)

    intro_start = raw.find("CHARACTER HOOKS")
    intro_end = raw.find("TIME OF YEAR")
    oakhurst_notes = clean_text(raw[intro_start:intro_end])
    db.execute(
        """INSERT INTO locations(
            id,name,area,parent_id,kind,player_description,
            gm_notes,map_asset,sort_order
        ) VALUES (?,?,?,?,?,?,?,?,?)""",
        (
            "oakhurst", "Oakhurst", "Oakhurst", None, "town",
            "A small town near the Old Road and the sunken fortress.",
            oakhurst_notes[:12000], "oakhurst_map", -1
        )
    )

    for number, name in ROOMS:
        area = "Grove Level" if number >= 47 else "Fortress Level"
        kind = "entrance" if number <= 2 else "dungeon"
        if number >= 47:
            kind = "grove"
        chunk = chunks[number]
        db.execute(
            """INSERT INTO locations(
                id,name,area,parent_id,kind,player_description,
                gm_notes,map_asset,sort_order
            ) VALUES (?,?,?,?,?,?,?,?,?)""",
            (
                f"area_{number}", name, area, None, kind,
                player_description(chunk, name),
                chunk[:30000],
                "fortress_map" if number < 47 else "grove_map",
                number
            )
        )

        treasure = extract_section(chunk, "Treasure", 2200)
        if treasure:
            db.execute(
                """INSERT INTO treasure(
                    id,location_id,name,rule_ref,quantity,hidden,
                    requirements_json,gm_notes
                ) VALUES (?,?,?,?,?,?,?,?)""",
                (
                    f"treasure_{number}", f"area_{number}",
                    f"Area {number} treasure", None, 1, 0, "[]", treasure
                )
            )

        for idx, (etype, el, section) in enumerate(encounter_sections(chunk), 1):
            db.execute(
                """INSERT INTO encounters(
                    id,location_id,name,difficulty,trigger_text,gm_notes
                ) VALUES (?,?,?,?,?,?)""",
                (
                    f"enc_{number}_{idx}", f"area_{number}",
                    f"{etype} encounter", f"3e EL {el}",
                    "When the party enters or triggers the encounter.",
                    section
                )
            )

    db.execute(
        """INSERT OR IGNORE INTO connections(
            from_id,to_id,travel_text,locked,requirements_json
        ) VALUES (?,?,?,?,?)""",
        ("oakhurst", "area_0", "Follow the Old Road to the ravine.", 0, "[]")
    )
    db.execute(
        """INSERT OR IGNORE INTO connections(
            from_id,to_id,travel_text,locked,requirements_json
        ) VALUES (?,?,?,?,?)""",
        ("area_0", "oakhurst", "Return along the Old Road.", 0, "[]")
    )

    manual_edges = [
        (0, 1, "Descend from the ravine to the sandy ledge."),
        (0, 2, "Descend the rope toward the citadel foyer."),
        (1, 2, "Follow the rough stairs deeper into the ravine."),
    ]
    for a, b, travel_text in manual_edges:
        for from_id, to_id in ((a, b), (b, a)):
            db.execute(
                """INSERT OR IGNORE INTO connections(
                    from_id,to_id,travel_text,locked,requirements_json
                ) VALUES (?,?,?,?,?)""",
                (
                    f"area_{from_id}", f"area_{to_id}",
                    travel_text, 0, "[]"
                )
            )

    for number, chunk in chunks.items():
        for target in connection_mentions(chunk):
            if target == number:
                continue
            for a, b in ((number, target), (target, number)):
                db.execute(
                    """INSERT OR IGNORE INTO connections(
                        from_id,to_id,travel_text,locked,requirements_json
                    ) VALUES (?,?,?,?,?)""",
                    (
                        f"area_{a}", f"area_{b}",
                        "Connection inferred from the source text.",
                        0, "[]"
                    )
                )

    for name in KEY_NPCS:
        room = best_area_for_name(chunks, name)
        inferred = f"area_{room}" if room is not None else "oakhurst"
        location = NPC_LOCATIONS.get(name, inferred)
        db.execute(
            """INSERT INTO npcs(
                id,name,role,location_id,description,gm_notes,
                creature_rule_ref,disposition
            ) VALUES (?,?,?,?,?,?,?,?)""",
            (
                re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_"),
                name, "source NPC", location, "", "",
                None, "unknown"
            )
        )

    add_quests(db)
    db.commit()
    db.execute("VACUUM")
    db.close()
    make_manifest()
    print("built", DB_PATH)

def add_quests(db):
    quests = [
        (
            "missing_hucreles",
            "The Missing Hucreles",
            "Find Talgen and Sharwyn Hucrele, or recover their signet rings."
        ),
        (
            "fruit_mystery",
            "The Enchanted Fruit",
            "Discover the source and truth of the enchanted fruit."
        ),
        (
            "lost_dragon",
            "The Kobolds' Lost Dragon",
            "A kobold bargain can lead the party to retrieve their lost pet."
        ),
    ]
    for qid, title, description in quests:
        db.execute(
            "INSERT INTO quests(id,title,description,gm_notes) VALUES (?,?,?,?)",
            (qid, title, description, "")
        )

if __name__ == "__main__":
    build()
