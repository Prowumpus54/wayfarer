package com.wayfarer.rpg

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CampaignStateStore(
    context: Context,
    campaignId: String
) {
    private val prefs = context.getSharedPreferences(
        "wayfarer_campaign_$campaignId",
        Context.MODE_PRIVATE
    )

    fun loadLocation(defaultId: String): String =
        prefs.getString("location_id", defaultId) ?: defaultId

    fun saveLocation(locationId: String) {
        prefs.edit().putString("location_id", locationId).apply()
    }

    fun loadEvents(): List<GameEvent> {
        val raw = prefs.getString("events_json", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                GameEvent(
                    title = item.optString("title"),
                    body = item.optString("body"),
                    type = item.optString("type"),
                    timeLabel = item.optString("timeLabel", "Now"),
                    id = item.optString("id").ifBlank {
                        java.util.UUID.randomUUID().toString()
                    }
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveEvents(events: List<GameEvent>) {
        val array = JSONArray()
        events.take(250).forEach { event ->
            array.put(
                JSONObject()
                    .put("title", event.title)
                    .put("body", event.body)
                    .put("type", event.type)
                    .put("timeLabel", event.timeLabel)
                    .put("id", event.id)
            )
        }
        prefs.edit().putString("events_json", array.toString()).apply()
    }

    fun loadDiscovered(defaultId: String): Set<String> {
        val stored = prefs.getStringSet("discovered_locations", null)
        return stored?.toSet() ?: setOf(defaultId)
    }

    fun saveDiscovered(ids: Set<String>) {
        prefs.edit().putStringSet(
            "discovered_locations",
            ids.toSet()
        ).apply()
    }

    fun loadPlayerNote(): String =
        prefs.getString("player_note", "") ?: ""

    fun savePlayerNote(note: String) {
        prefs.edit().putString("player_note", note).apply()
    }
}
