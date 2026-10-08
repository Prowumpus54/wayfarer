# Visual Map & Asset Architecture

Status: Design specification
Target: LoreWise map engine, visual asset pipeline, and live combat effects
Initial reference module: The Sunless Citadel

## 1. Design goals

LoreWise must render a live, persistent, rules-aware map without making generated artwork authoritative.

The visual system must:
- keep world topology, positions, discovery, fog, collision, encounter state, and PF1 combat geometry authoritative in Android;
- support exploration-scale and tactical 5-foot-grid rendering from the same map state;
- allow desktop-generated tiles, tokens, portraits, effects, room art, and map accents to be synchronized to the phone and cached for offline use;
- support procedural maps built from reusable theme packs plus optional map-specific accent packs;
- support animated weapon, spell, condition, environmental, and impact effects;
- degrade gracefully when an art asset is missing by using a generic fallback without breaking gameplay;
- allow BuildWise/developer tooling to inspect asset versions, missing assets, load failures, and generation provenance.

Generated art may change presentation. It must never change authoritative map geometry, rules, hidden/discovered state, or creature statistics unless a validated game-state operation separately commits that change.

## 2. Runtime ownership

### MapStateEngine
Owns:
- current world/area/room;
- area and room graph;
- valid connections;
- room polygons and walkable geometry;
- doors, stairs, portals, elevation changes, and blocking edges;
- party/token authoritative positions;
- discovered/visited/searched state;
- fog-of-war visibility;
- encounter attachment;
- tactical grid coordinates;
- line-of-sight and movement geometry inputs.

### MapRenderEngine
Owns presentation only:
- camera;
- pan/zoom;
- render-layer ordering;
- tile/background drawing;
- props/decals;
- tokens;
- fog overlays;
- selection/range/path overlays;
- effect playback surfaces.

### VisualAssetEngine
Owns:
- asset manifest parsing;
- local asset registry;
- lookup by asset ID and semantic tags;
- local cache;
- version/hash checks;
- fallback selection;
- texture/image decode and memory lifecycle;
- asset-pack enable/disable/version state.

### EffectEngine
Owns:
- weapon-family animation playback;
- spell animation playback;
- projectiles;
- impacts;
- cones, lines, bursts, auras, beams, ground zones;
- condition/status overlays;
- environmental effects;
- animation timing and cleanup.

The EffectEngine never decides whether an attack hits, how much damage occurs, whether a spell is legal, or whether an effect applies. It visualizes a result already produced by the rules/state engines.

### Desktop AssetBuildPipeline
Owns:
- generated/imported source art;
- validation;
- normalization;
- atlas packing;
- compression;
- manifest generation;
- hashing/versioning;
- pack creation;
- publishing to BuildWise sync, cloud distribution, or an importable LoreWise pack.

## 3. World/map data model

The map system should use stable IDs rather than prose names.

### WorldMap
Fields:
- id
- moduleId
- name
- themePackId
- accentPackIds
- areas
- version

### MapArea
Fields:
- id
- name
- floor/elevation
- roomIds
- backgroundAssetId optional
- bounds
- lighting profile

### MapRoom
Fields:
- id
- areaId
- name
- polygon
- grid origin
- floorAsset/tag
- wallAsset/tag
- room type
- discovered
- visited
- searched
- encounterId optional
- scene/module reference
- prop placements
- decal placements

### MapConnection
Fields:
- id
- fromRoomId
- toRoomId
- type: open passage / door / secret door / stairs / ladder / cliff / portal
- traversal requirements
- state: unknown / discovered / closed / open / locked / blocked
- geometry anchor

### MapToken
Fields:
- id
- entityId
- entityType: player / NPC / creature / summon / object
- tokenAssetId
- roomId
- x/y position
- grid footprint
- facing optional
- elevation
- visibility
- alive/defeated state
- condition visual IDs

## 4. Two map scales

### Exploration mode
Optimized for navigation and discovery.

Uses:
- room/zone positions;
- simplified token positions;
- discovered/unknown areas;
- room labels;
- routes and connections;
- encounter markers only when player-visible.

Exact 5-foot positioning is not required.

### Tactical mode
Activated when exact PF1 positioning matters.

Uses:
- 5-foot grid;
- exact creature footprints;
- movement range;
- difficult terrain;
- reach;
- line of sight;
- cover;
- spell/weapon range;
- area templates;
- threatened squares;
- path previews.

Both modes operate on the same MapState. Entering tactical mode must not create a second disconnected copy of the room state.

## 5. Render layer order

Recommended back-to-front order:

1. base background / parchment / terrain plate
2. floor tiles
3. fixed room geometry
4. walls / cliffs / structural edges
5. doors / gates / stairs
6. static props
7. decals / stains / rubble / vegetation
8. environmental animated layers
9. hidden/discovered masking
10. fog of war
11. ground spell zones
12. tokens
13. token condition overlays
14. projectiles / weapon trails / spell effects
15. impact effects
16. selection rings / movement path / ranges / targeting
17. labels and lightweight HUD overlays

## 6. Asset pack structure

Reference desktop source layout:

LoreWiseAssets/
  packs/
    core-dungeon/
      manifest.json
      floors/
      walls/
      doors/
      props/
      decals/
      effects/
      ui/
    sunless-citadel/
      manifest.json
      accents/
      unique-props/
      tokens/
      portraits/
      room-art/
  staging/
  generated/
  compiled/

Reference phone cache layout is implementation-specific, but packs must remain identifiable by pack ID, version, and hash.

## 7. Asset manifest

Each compiled asset pack must expose one versioned manifest.

Example:

{
  "schemaVersion": 1,
  "packId": "sunless-citadel-visuals",
  "version": "1.0.0",
  "theme": "ruined-citadel",
  "assets": [
    {
      "id": "floor.flagstone.damp.01",
      "type": "tile",
      "file": "floors/flagstone_damp_01.webp",
      "width": 256,
      "height": 256,
      "sha256": "...",
      "tags": ["floor", "stone", "damp", "dungeon"],
      "tileable": true
    },
    {
      "id": "token.twig-blight.01",
      "type": "token",
      "file": "tokens/twig_bligh_01.webp",
      "width": 512,
      "height": 512,
      "sha256": "...",
      "tags": ["creature", "plant", "twig-blight"],
      "anchor": [0.5, 0.82]
    }
  ]
}

Required per-asset fields:
- id
- type
- file
- width/height
- hash
- semantic tags

Optional fields:
- tileable
- anchor
- footprint
- animation metadata
- blend mode
- loop behavior
- source/generation metadata
- fallback asset ID

## 8. Recommended source sizes

### Tiles
Default source:
- 256 x 256 px

High-detail tiles:
- 512 x 512 px

Use seamless/tileable generation for reusable floors and large continuous textures.

### Tokens
Standard:
- 512 x 512 px

Minimum:
- 256 x 256 px

Large/boss source:
- 768-1024 px square when useful.

Tokens should use transparent backgrounds and a consistent anchor convention.

### Props and decals
Typical:
- 128-512 px depending on physical/map size.

Large structural props may exceed 512 px.

### Spell and attack sprite atlases
Typical:
- 512-1024 px atlas.

Large/high-frame-count effects may use:
- 2048 x 2048 px atlas sparingly.

### Portraits
Recommended:
- 768 x 1024 px portrait source
or
- 1024 x 1024 px square portrait source.

### Room/scene art
Recommended:
- approximately 2048 px on the long edge.

Room/scene art is decorative and does not define collision or topology.

## 9. Runtime image formats

Preferred:
- WebP for most static transparent/nontransparent assets;
- PNG during authoring when lossless transparency is useful;
- sprite atlas + JSON metadata for frame animation.

Future optimization candidate:
- KTX2/Basis for GPU-friendly texture compression if profiling shows image decode/memory pressure warrants it.

Do not use animated GIF as the core effect format.

## 10. Animation system

LoreWise should combine deterministic procedural animation with sprite assets.

### Reusable procedural effect primitives
- slash arc
- thrust line
- projectile
- beam
- cone
- burst
- radial pulse
- aura
- ground circle
- trail
- screen/token flash
- particles
- lingering zone

Each primitive accepts parameters such as:
- duration
- scale
- angle
- origin/target
- tint
- sprite/atlas
- particle count
- speed
- blend mode
- loop count

### Weapon families
Initial families:
- sword slash
- axe cleave
- mace/hammer impact
- spear/polearm thrust
- dagger stab
- bow shot
- crossbow shot
- thrown weapon arc
- staff swing
- wand/ray cast

Individual weapons inherit a family and may override trail, impact, timing, sound, or sprite.

This produces distinct weapon identity without requiring a custom animation implementation for every weapon.

### Spell families
Initial visual primitives:
- projectile
- ray/beam
- cone
- burst
- line
- aura
- ground zone
- summon
- heal
- shield/buff
- debuff/condition
- elemental impact

A spell definition maps rules output to one or more visual effects.

## 11. Animation metadata

Example sprite animation metadata:

{
  "id": "fx.weapon.sword.slash.01",
  "type": "animation",
  "atlas": "effects/sword_slash_01.webp",
  "frameWidth": 128,
  "frameHeight": 128,
  "frameCount": 12,
  "fps": 24,
  "loop": false,
  "blendMode": "add",
  "anchor": [0.5, 0.5],
  "tags": ["weapon", "sword", "slash"]
}

Example procedural effect metadata:

{
  "id": "fx.spell.fire.small-burst",
  "type": "particleEffect",
  "sprite": "effects/fire_particle_01.webp",
  "durationMs": 900,
  "spawnRate": 20,
  "scaleRange": [0.5, 1.3],
  "speedRange": [20, 80],
  "tags": ["spell", "fire", "burst"]
}

## 12. Tileset strategy

Do not generate an entirely independent tileset for every room.

Use two levels:

### Reusable theme packs
Examples:
- village
- forest
- ravine
- ruined-citadel
- goblin-warrens
- cave
- crypt
- temple
- sewer
- laboratory

A theme pack supplies common:
- floors;
- walls;
- doors;
- stairs;
- structural props;
- clutter;
- decals;
- light fixtures;
- environmental effects.

### Map-specific accent packs
A module/map can add:
- heraldry;
- faction banners;
- special statues;
- unique altars;
- boss-room geometry art;
- unusual doors;
- unique vegetation;
- special floor motifs;
- module-specific props.

The procedural generator combines reusable geometry logic with theme assets and map-specific accents.

## 13. AI-generated unique map packs

The PC may generate an accent/style pack for a new map.

Recommended generated batch per map:
- 4-12 floor/material textures;
- 4-8 wall/material textures;
- 2-6 prop families;
- 3-10 decals;
- 1-3 environmental overlays;
- optional unique doors/altars/statues;
- optional room/scene illustrations.

Generated visuals must be validated for:
- dimensions;
- transparent background where required;
- seamless tiling where required;
- alpha-edge quality;
- semantic tag assignment;
- no accidental baked-in grid/labels unless explicitly intended.

The procedural map generator owns geometry. AI-generated images do not determine door placement, walkability, collision, or room connections.

## 14. Desktop AssetBuildPipeline

Pipeline stages:

1. Ingest
   - generated images;
   - hand-authored assets;
   - imported/licensed source assets.

2. Validate
   - dimensions;
   - file type;
   - alpha;
   - naming;
   - duplicate IDs;
   - required metadata.

3. Normalize
   - crop/pad;
   - anchor alignment;
   - resolution normalization;
   - transparent-edge cleanup;
   - optional seamless correction.

4. Optimize
   - convert static assets to WebP;
   - generate device-appropriate downscaled variants when useful.

5. Pack
   - optionally atlas small sprites/effects;
   - preserve independent large assets when atlas packing is inefficient.

6. Manifest
   - emit IDs, paths, dimensions, tags, hashes, animation metadata, dependencies, pack version.

7. Verify
   - manifest/file consistency;
   - hash validation;
   - no missing referenced assets;
   - atlas frame bounds.

8. Publish
   - BuildWise/Tailscale development sync;
   - importable .lorepack bundle;
   - optional cloud asset distribution.

## 15. Sync and offline behavior

### Development path
Preferred first implementation:
- desktop AssetBuildPipeline compiles a pack;
- BuildWise exposes the pack manifest and files over the authorized development path;
- phone compares pack/version/hashes;
- phone downloads only missing/changed files;
- files are persisted in local cache;
- map renderer reads only local cached assets during play.

### Offline behavior
Gameplay must not depend on live asset connectivity.

If an expected asset is missing:
1. use a semantically appropriate generic fallback;
2. record a diagnostic;
3. queue the missing asset for later sync;
4. continue gameplay.

### Future distribution
Support:
- manual .lorepack import;
- cloud-hosted published visual packs;
- prefetch of likely upcoming module areas.

## 16. Cache behavior

VisualAssetEngine should maintain:
- installed pack versions;
- asset hash index;
- disk-size accounting;
- last-used timestamp;
- protected core/fallback assets;
- optional per-campaign pinning.

Memory cache:
- load only nearby/current-scene textures and effects;
- release large unused room art aggressively;
- retain common UI/tokens/effects as profiling permits.

Disk cache:
- never evict assets required by the active offline campaign without explicit policy;
- generated/re-downloadable scene art may be lower priority than core tiles/tokens.

## 17. Sunless Citadel MVP visual inventory

Goal: enough visual coverage to prove the engine without creating hundreds of bespoke assets.

### Environment: approximately 46 assets
- 12 floor textures;
- 10 wall/edge textures;
- 6 door/gate/stair assets;
- 12 props;
- 6 decals/overlays.

### Tokens: approximately 26-36 assets
- Thorne token;
- optional party tokens;
- 15-20 early-module creature tokens;
- 5-10 important NPC tokens;
- generic unknown-creature fallback.

### Effects: approximately 16-24 assets/definitions
Weapon:
- sword slash;
- axe cleave;
- blunt impact;
- spear thrust;
- dagger stab;
- bow shot;
- crossbow shot;
- generic projectile/impact.

Spell:
- fire burst;
- cold burst;
- acid splash;
- electric arc/impact;
- healing glow;
- buff aura;
- debuff pulse;
- magic projectile/ray;
- ground area;
- summon effect.

Conditions:
- selected;
- threatened;
- prone;
- stunned/dazed;
- poisoned/diseased;
- unconscious/defeated.

### Portrait/scene art: approximately 10-25 optional assets
- key NPC portraits;
- player portrait;
- major room/area scene cards.

Practical MVP total:
- roughly 100-150 source assets/definitions for the first visually convincing Sunless Citadel slice.

A richer first campaign pack can grow toward roughly 150-250 assets without changing the engine architecture.

## 18. Procedural generation contract

Procedural generation operates on semantic and geometric data.

Example request:

{
  "theme": "ruined-citadel",
  "roomType": "chapel",
  "sizeFeet": [40, 60],
  "requiredConnections": ["south-door", "east-passage"],
  "features": ["north-altar", "collapsed-west-wall"],
  "difficulty": "exploration"
}

The deterministic generator:
- creates legal room geometry;
- preserves required exits;
- applies grid alignment;
- assigns collision;
- places structural features;
- selects tagged theme assets;
- places optional clutter/decal variants;
- emits a MapRoom plus placements.

Gemma may propose the semantic description. It must not directly write arbitrary final polygons into persistent state without map-engine validation.

## 19. Live effect flow

Example sword attack:

1. Player selects attack and target.
2. CombatRulesEngine validates action.
3. Android rolls/resolves attack.
4. State engine commits damage/conditions.
5. EffectEngine receives a visual event:
   - actor;
   - target;
   - weapon family;
   - hit/miss/critical result.
6. EffectEngine plays:
   - movement/attack anticipation if configured;
   - sword slash arc;
   - hit spark or miss trail;
   - critical emphasis when applicable.
7. Combat log and GM narration use the already-authoritative result.

Example fire spell:

1. Spell legality/resource usage validated.
2. Targets/area resolved.
3. Damage/save/effects resolved.
4. State committed.
5. EffectEngine receives spell visual definition + affected map coordinates.
6. Appropriate projectile/cone/burst/ground effect plays.

Visual playback failure must never roll back or block committed mechanics.

## 20. AI image generation role

Desktop image generation is appropriate for:
- tokens;
- portraits;
- room scene cards;
- seamless materials;
- props;
- decals;
- map-specific motifs;
- effect source sprites.

Do not ask an image model to generate the authoritative tactical map as a single raster image.

If an AI-generated room background is used, deterministic walls/grid/doors/tokens remain overlaid from MapState.

## 21. Diagnostics and Wise 1.1 observability

Asset operations should record:
- pack ID/version;
- asset ID;
- cache hit/miss;
- load/decode duration;
- download/sync duration;
- fallback use;
- validation failure;
- atlas failure;
- memory-pressure eviction;
- effect playback failure.

Image-generation workflows should have a protected generation trace containing:
- generation request/prompt;
- model/workflow ID;
- output asset IDs;
- generation duration/status;
- source-to-compiled asset relationship.

Do not record authentication headers or credentials.

## 22. Initial implementation sequence

### Phase A: authoritative map core
- WorldMap / MapArea / MapRoom / MapConnection / MapToken models;
- persistent current room;
- Oakhurst -> Old Road -> Ravine -> Citadel entry;
- discovery/visited flags;
- valid movement transitions;
- Map tab renders schematic rooms/connections.

### Phase B: basic visual engine
- VisualAssetEngine;
- pack manifest;
- local cache;
- floor/wall/token drawing;
- fallback assets;
- pan/zoom;
- fog of war.

### Phase C: Sunless Citadel visual pack
- core ruined-citadel tiles;
- early tokens;
- props;
- map-specific accents;
- BuildWise development sync.

### Phase D: tactical mode
- 5-foot grid;
- exact token footprints;
- path/movement overlays;
- targeting;
- LOS/range hooks;
- tactical fog/visibility.

### Phase E: live effects
- weapon-family effect primitives;
- spell-family effect primitives;
- conditions;
- hit/miss/critical presentation;
- effect manifest support.

### Phase F: desktop generation pipeline
- generation staging folder;
- validation;
- normalization;
- WebP conversion;
- atlas builder;
- manifest/hash generation;
- .lorepack creation;
- BuildWise publish/sync.

### Phase G: procedural generation
- semantic room specification;
- deterministic geometry generator;
- theme/tag-driven asset placement;
- unique accent-pack generation;
- validation and preview.

## 23. Acceptance criteria for the first complete vertical slice

A successful first slice should demonstrate all of the following on a Pixel:

1. Thorne starts in Oakhurst.
2. Map tab shows discovered Oakhurst node/area.
3. Moving along the Old Road updates authoritative location.
4. Ravine discovery appears without exposing hidden Citadel rooms.
5. Entering the Citadel updates current room and GM context.
6. A local cached tileset renders the entrance.
7. Thorne has a visible map token.
8. An encounter can add a creature token.
9. A longsword attack plays a sword-family animation after mechanical resolution.
10. At least one spell/ability effect renders through EffectEngine.
11. Fog/discovery survives app restart.
12. The entire sequence works with the desktop disconnected once required assets are cached.
13. Missing art falls back gracefully without breaking play.
14. BuildWise/Diagnostics can identify which visual pack and asset versions were used.
