package com.wayfarer.rpg

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.io.File

data class AdventureModuleManifest(
    val id: String,
    val title: String,
    val version: String,
    val ruleset: String,
    val description: String,
    val startingLocation: String,
    val minLevel: Int,
    val maxLevel: Int
)

data class ModuleLocation(
    val id: String,
    val name: String,
    val area: String?,
    val kind: String?,
    val playerDescription: String,
    val gmNotes: String
)

data class ModuleDestination(
    val id: String,
    val name: String,
    val travelText: String
)

data class ModuleNpcSummary(
    val name: String,
    val role: String,
    val disposition: String
)

data class ModuleEncounterSummary(
    val name: String,
    val difficulty: String,
    val triggerText: String,
    val gmNotes: String
)

data class ModuleQuestSummary(
    val id: String,
    val title: String,
    val description: String
)

data class ModuleSceneContext(
    val location: ModuleLocation,
    val destinations: List<ModuleDestination>,
    val npcs: List<ModuleNpcSummary>,
    val encounters: List<ModuleEncounterSummary>
)

class AdventureModuleRepository(
    private val context: Context
) {
    private val root = File(context.filesDir, "wayfarer_modules").apply {
        mkdirs()
    }

    fun ensureBundledModuleInstalled(moduleId: String): AdventureModuleManifest? {
        val assetRoot = "modules/$moduleId"
        val manifestText = runCatching {
            context.assets.open("$assetRoot/manifest.json")
                .bufferedReader()
                .use { it.readText() }
        }.getOrNull() ?: return manifest(moduleId)

        val bundled = parseManifest(JSONObject(manifestText)) ?: return manifest(moduleId)
        val targetDir = File(root, moduleId).apply { mkdirs() }
        val installed = loadManifest(targetDir)

        if (installed?.version != bundled.version ||
            !File(targetDir, "module.sqlite").exists()
        ) {
            File(targetDir, "manifest.json").writeText(manifestText)
            context.assets.open("$assetRoot/module.sqlite").use { input ->
                File(targetDir, "module.sqlite").outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return loadManifest(targetDir)
    }

    fun installedModules(): List<AdventureModuleManifest> =
        root.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { loadManifest(it) }
            ?.sortedBy { it.title }
            .orEmpty()

    fun manifest(moduleId: String): AdventureModuleManifest? =
        loadManifest(File(root, moduleId))

    fun openModule(moduleId: String): SQLiteDatabase? {
        val file = File(File(root, moduleId), "module.sqlite")
        if (!file.exists()) return null
        return SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
    }

    fun location(moduleId: String, locationId: String): ModuleLocation? {
        val db = openModule(moduleId) ?: return null
        return try {
            db.rawQuery(
                """SELECT id,name,area,kind,player_description,gm_notes
                   FROM locations WHERE id=? LIMIT 1""".trimIndent(),
                arrayOf(locationId)
            ).use { cursor ->
                if (!cursor.moveToFirst()) null else ModuleLocation(
                    id = cursor.getString(0),
                    name = cursor.getString(1),
                    area = cursor.getString(2),
                    kind = cursor.getString(3),
                    playerDescription = cursor.getString(4) ?: "",
                    gmNotes = cursor.getString(5) ?: ""
                )
            }
        } finally {
            db.close()
        }
    }

    fun destinations(moduleId: String, locationId: String): List<ModuleDestination> {
        val db = openModule(moduleId) ?: return emptyList()
        return try {
            val out = mutableListOf<ModuleDestination>()
            db.rawQuery(
                """SELECT l.id,l.name,c.travel_text
                   FROM connections c
                   JOIN locations l ON l.id=c.to_id
                   WHERE c.from_id=? AND c.locked=0
                   ORDER BY l.sort_order,l.name""".trimIndent(),
                arrayOf(locationId)
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    out += ModuleDestination(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        travelText = cursor.getString(2) ?: ""
                    )
                }
            }
            out.distinctBy { it.id }
        } finally {
            db.close()
        }
    }

    fun npcsAt(moduleId: String, locationId: String): List<ModuleNpcSummary> {
        val db = openModule(moduleId) ?: return emptyList()
        return try {
            val out = mutableListOf<ModuleNpcSummary>()
            db.rawQuery(
                """SELECT name,role,disposition
                   FROM npcs WHERE location_id=? ORDER BY name""".trimIndent(),
                arrayOf(locationId)
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    out += ModuleNpcSummary(
                        name = cursor.getString(0),
                        role = cursor.getString(1) ?: "",
                        disposition = cursor.getString(2) ?: ""
                    )
                }
            }
            out
        } finally {
            db.close()
        }
    }

    fun encountersAt(moduleId: String, locationId: String): List<ModuleEncounterSummary> {
        val db = openModule(moduleId) ?: return emptyList()
        return try {
            val out = mutableListOf<ModuleEncounterSummary>()
            db.rawQuery(
                """SELECT name,difficulty,trigger_text,gm_notes
                   FROM encounters WHERE location_id=? ORDER BY id""".trimIndent(),
                arrayOf(locationId)
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    out += ModuleEncounterSummary(
                        name = cursor.getString(0),
                        difficulty = cursor.getString(1) ?: "",
                        triggerText = cursor.getString(2) ?: "",
                        gmNotes = cursor.getString(3) ?: ""
                    )
                }
            }
            out
        } finally {
            db.close()
        }
    }

    fun sceneContext(moduleId: String, locationId: String): ModuleSceneContext? {
        val location = location(moduleId, locationId) ?: return null
        return ModuleSceneContext(
            location = location,
            destinations = destinations(moduleId, locationId),
            npcs = npcsAt(moduleId, locationId),
            encounters = encountersAt(moduleId, locationId)
        )
    }

    fun quests(moduleId: String): List<ModuleQuestSummary> {
        val db = openModule(moduleId) ?: return emptyList()
        return try {
            val out = mutableListOf<ModuleQuestSummary>()
            db.rawQuery(
                "SELECT id,title,description FROM quests ORDER BY title",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    out += ModuleQuestSummary(
                        id = cursor.getString(0),
                        title = cursor.getString(1),
                        description = cursor.getString(2) ?: ""
                    )
                }
            }
            out
        } finally {
            db.close()
        }
    }

    fun campaignStateDirectory(
        moduleId: String,
        campaignId: String
    ): File {
        return File(
            contextStateRoot(),
            moduleId + File.separator + campaignId
        ).apply { mkdirs() }
    }

    private fun contextStateRoot(): File =
        File(root.parentFile, "campaign_state").apply { mkdirs() }

    private fun loadManifest(directory: File): AdventureModuleManifest? {
        val file = File(directory, "manifest.json")
        if (!file.exists()) return null
        return runCatching {
            parseManifest(JSONObject(file.readText()))
        }.getOrNull()
    }

    private fun parseManifest(json: JSONObject): AdventureModuleManifest? {
        return runCatching {
            AdventureModuleManifest(
                id = json.getString("id"),
                title = json.getString("title"),
                version = json.optString("version", "1"),
                ruleset = json.optString("ruleset", "pf2e"),
                description = json.optString("description"),
                startingLocation = json.optString("startingLocation"),
                minLevel = json.optInt("minLevel", 1),
                maxLevel = json.optInt("maxLevel", 20)
            )
        }.getOrNull()
    }
}
