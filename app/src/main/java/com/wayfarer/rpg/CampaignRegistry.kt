package com.wayfarer.rpg

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CampaignProfile(
    val id: String,
    val name: String,
    val moduleId: String,
    val ownerUid: String? = null,
    val inviteCode: String? = null,
    val joined: Boolean = false
)

class CampaignRegistry(context: Context) {
    private val prefs = context.getSharedPreferences(
        "wayfarer_campaign_registry",
        Context.MODE_PRIVATE
    )

    fun activeCampaign(moduleId: String, moduleTitle: String): CampaignProfile {
        val campaigns = load()
        val activeId = prefs.getString("active_campaign_id", null)
        campaigns.firstOrNull { it.id == activeId }?.let { return it }
        campaigns.firstOrNull()?.let {
            prefs.edit().putString("active_campaign_id", it.id).apply()
            return it
        }

        // Keep the original campaign ID for the first save so existing
        // pre-multiplayer progress migrates without losing state.
        val created = CampaignProfile(
            id = moduleId,
            name = moduleTitle,
            moduleId = moduleId
        )
        save(listOf(created))
        prefs.edit().putString("active_campaign_id", created.id).apply()
        return created
    }

    fun load(): List<CampaignProfile> {
        val raw = prefs.getString("campaigns_json", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                CampaignProfile(
                    id = item.getString("id"),
                    name = item.optString("name", "Campaign"),
                    moduleId = item.optString("moduleId", "sunless_citadel"),
                    ownerUid = item.optString("ownerUid").takeIf { it.isNotBlank() },
                    inviteCode = item.optString("inviteCode").takeIf { it.isNotBlank() },
                    joined = item.optBoolean("joined", false)
                )
            }
        }.getOrDefault(emptyList())
    }
    fun save(campaigns: List<CampaignProfile>) {
        val array = JSONArray()
        campaigns.forEach { campaign ->
            array.put(
                JSONObject()
                    .put("id", campaign.id)
                    .put("name", campaign.name)
                    .put("moduleId", campaign.moduleId)
                    .put("ownerUid", campaign.ownerUid ?: "")
                    .put("inviteCode", campaign.inviteCode ?: "")
                    .put("joined", campaign.joined)
            )
        }
        prefs.edit().putString("campaigns_json", array.toString()).apply()
    }

    fun createLocal(name: String, moduleId: String): CampaignProfile {
        val created = CampaignProfile(
            id = "local_" + UUID.randomUUID().toString(),
            name = name.ifBlank { "New Campaign" },
            moduleId = moduleId
        )
        save(load() + created)
        select(created.id)
        return created
    }

    fun upsert(profile: CampaignProfile) {
        val campaigns = load().toMutableList()
        val index = campaigns.indexOfFirst { it.id == profile.id }
        if (index >= 0) campaigns[index] = profile else campaigns.add(profile)
        save(campaigns)
    }

    fun select(campaignId: String) {
        require(load().any { it.id == campaignId })
        prefs.edit().putString("active_campaign_id", campaignId).apply()
    }
}
