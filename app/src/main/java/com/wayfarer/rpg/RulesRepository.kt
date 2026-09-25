package com.wayfarer.rpg

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import java.io.File

data class RuleEntrySummary(
    val uid: String,
    val name: String,
    val level: Int,
    val category: String?,
    val rarity: String?,
    val traits: List<String>,
    val traditions: List<String>,
    val description: String,
    val sourceTitle: String?,
    val remaster: Boolean
)

data class ClassFeatureGain(
    val name: String,
    val level: Int
)

data class ClassProgression(
    val name: String,
    val hpPerLevel: Int,
    val keyAbilities: List<String>,
    val classFeatLevels: List<Int>,
    val skillFeatLevels: List<Int>,
    val generalFeatLevels: List<Int>,
    val ancestryFeatLevels: List<Int>,
    val skillIncreaseLevels: List<Int>,
    val features: List<ClassFeatureGain>
)

class RulesRepository(context: Context) {
    private val dbName = "wayfarer_rules_v1.sqlite"
    private val dbFile = File(context.filesDir, dbName)
    private val database: SQLiteDatabase

    init {
        if (!dbFile.exists()) {
            context.assets.open("wayfarer_rules.sqlite").use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        database = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
    }

    fun close() = database.close()

    fun count(kind: String): Int {
        database.rawQuery(
            "SELECT COUNT(*) FROM entries WHERE kind=?",
            arrayOf(kind)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    fun names(
        kind: String,
        remasterOnly: Boolean = true,
        maxLevel: Int? = null,
        category: String? = null,
        limit: Int = 10000
    ): List<String> {
        val where = mutableListOf("kind=?")
        val args = mutableListOf(kind)
        if (remasterOnly) where += "remaster=1"
        if (maxLevel != null) {
            where += "level<=?"
            args += maxLevel.toString()
        }
        if (category != null) {
            where += "category=?"
            args += category
        }
        val sql = "SELECT DISTINCT name FROM entries WHERE " +
            where.joinToString(" AND ") +
            " ORDER BY name_search LIMIT " + limit
        return queryNames(sql, args.toTypedArray())
    }

    fun featEntries(
        featType: String,
        maxLevel: Int,
        className: String? = null,
        ancestry: String? = null
    ): List<RuleEntrySummary> {
        val trait = when (featType) {
            "class" -> className?.lowercase()
            "ancestry" -> ancestry?.lowercase()
            else -> null
        }
        return search(
            kind = "feat",
            query = "",
            maxLevel = maxLevel,
            category = featType,
            requiredTrait = trait,
            limit = 5000
        )
    }

    fun featNames(
        featType: String,
        maxLevel: Int,
        className: String? = null,
        ancestry: String? = null
    ): List<String> {
        val base = names(
            kind = "feat",
            remasterOnly = true,
            maxLevel = maxLevel,
            category = featType,
            limit = 10000
        )
        if (featType != "class" && featType != "ancestry") return base

        val trait = when (featType) {
            "class" -> className?.lowercase()
            "ancestry" -> ancestry?.lowercase()
            else -> null
        } ?: return base

        return search(
            kind = "feat",
            query = "",
            maxLevel = maxLevel,
            category = featType,
            requiredTrait = trait,
            limit = 2000
        ).map { it.name }.distinct().sorted()
    }

    fun spellNames(
        rank: Int,
        tradition: String?,
        includeLegacy: Boolean = false
    ): List<String> {
        return search(
            kind = "spell",
            query = "",
            maxLevel = rank,
            exactLevel = rank,
            remasterOnly = !includeLegacy,
            requiredTradition = tradition?.lowercase(),
            limit = 5000
        ).map { it.name }.distinct().sorted()
    }

    fun search(
        kind: String,
        query: String,
        maxLevel: Int? = null,
        exactLevel: Int? = null,
        category: String? = null,
        requiredTrait: String? = null,
        requiredTradition: String? = null,
        remasterOnly: Boolean = true,
        limit: Int = 250
    ): List<RuleEntrySummary> {

        val where = mutableListOf("kind=?")
        val args = mutableListOf(kind)
        if (query.isNotBlank()) {
            where += "name_search LIKE ?"
            args += "%" + query.lowercase() + "%"
        }
        if (remasterOnly) where += "remaster=1"
        if (maxLevel != null) {
            where += "level<=?"
            args += maxLevel.toString()
        }
        if (exactLevel != null) {
            where += "level=?"
            args += exactLevel.toString()
        }
        if (category != null) {
            where += "category=?"
            args += category
        }
        val sql = "SELECT uid,name,level,category,rarity,traits_json," +
            "traditions_json,description,source_title,remaster FROM entries WHERE " +
            where.joinToString(" AND ") +
            " ORDER BY level,name_search LIMIT " + limit

        val result = mutableListOf<RuleEntrySummary>()
        database.rawQuery(sql, args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                val item = RuleEntrySummary(
                    uid = cursor.getString(0),
                    name = cursor.getString(1),
                    level = cursor.getInt(2),
                    category = cursor.getString(3),
                    rarity = cursor.getString(4),
                    traits = jsonStrings(cursor.getString(5)),
                    traditions = jsonStrings(cursor.getString(6)),
                    description = cursor.getString(7) ?: "",
                    sourceTitle = cursor.getString(8),
                    remaster = cursor.getInt(9) == 1
                )
                if (requiredTrait != null &&
                    item.traits.none { it.equals(requiredTrait, true) }) continue
                if (requiredTradition != null &&
                    item.traditions.none { it.equals(requiredTradition, true) }) continue
                result += item
            }
        }
        return result
    }

    fun searchAll(
        query: String,
        remasterOnly: Boolean = false,
        limit: Int = 300
    ): List<RuleEntrySummary> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (query.isNotBlank()) {
            where += "(name_search LIKE ? OR description LIKE ?)"
            val like = "%" + query.lowercase() + "%"
            args += like
            args += like
        }
        if (remasterOnly) where += "remaster=1"
        val whereSql = if (where.isEmpty()) "" else
            " WHERE " + where.joinToString(" AND ")
        val sql = "SELECT uid,name,level,category,rarity,traits_json," +
            "traditions_json,description,source_title,remaster FROM entries" +
            whereSql + " ORDER BY kind,name_search LIMIT " + limit
        val out = mutableListOf<RuleEntrySummary>()
        database.rawQuery(sql, args.toTypedArray()).use { cursor ->
            while (cursor.moveToNext()) {
                out += RuleEntrySummary(
                    uid = cursor.getString(0),
                    name = cursor.getString(1),
                    level = cursor.getInt(2),
                    category = cursor.getString(3),
                    rarity = cursor.getString(4),
                    traits = jsonStrings(cursor.getString(5)),
                    traditions = jsonStrings(cursor.getString(6)),
                    description = cursor.getString(7) ?: "",
                    sourceTitle = cursor.getString(8),
                    remaster = cursor.getInt(9) == 1
                )
            }
        }
        return out
    }

    fun findByName(kind: String, name: String): RuleEntrySummary? {
        val results = search(
            kind = kind,
            query = name,
            remasterOnly = false,
            limit = 50
        )
        return results.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: results.firstOrNull()
    }

    fun findAnyByName(
        name: String,
        kinds: List<String>
    ): RuleEntrySummary? {
        for (kind in kinds) {
            findByName(kind, name)?.let { return it }
        }
        return null
    }

    fun classProgression(className: String): ClassProgression? {
        database.rawQuery(
            "SELECT name,hp,key_abilities_json,class_feat_levels_json," +
                "skill_feat_levels_json,general_feat_levels_json," +
                "ancestry_feat_levels_json,skill_increase_levels_json,features_json " +
                "FROM classes WHERE name=? AND remaster=1 LIMIT 1",
            arrayOf(className)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return ClassProgression(
                name = cursor.getString(0),
                hpPerLevel = cursor.getInt(1),
                keyAbilities = jsonStrings(cursor.getString(2)),
                classFeatLevels = jsonInts(cursor.getString(3)),
                skillFeatLevels = jsonInts(cursor.getString(4)),
                generalFeatLevels = jsonInts(cursor.getString(5)),
                ancestryFeatLevels = jsonInts(cursor.getString(6)),
                skillIncreaseLevels = jsonInts(cursor.getString(7)),
                features = jsonFeatures(cursor.getString(8))
            )
        }
    }

    private fun queryNames(sql: String, args: Array<String>): List<String> {
        val out = mutableListOf<String>()
        database.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) out += cursor.getString(0)
        }
        return out
    }

    private fun jsonStrings(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            List(array.length()) { index -> array.optString(index) }
                .filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun jsonInts(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            List(array.length()) { index -> array.optInt(index) }
        }.getOrDefault(emptyList())
    }

    private fun jsonFeatures(value: String?): List<ClassFeatureGain> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                ClassFeatureGain(
                    name = item.optString("name"),
                    level = item.optInt("level")
                )
            }.filter { it.name.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
