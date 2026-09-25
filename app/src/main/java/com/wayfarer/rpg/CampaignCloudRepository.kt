package com.wayfarer.rpg

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.security.SecureRandom

class CampaignCloudRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val random = SecureRandom()
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun loadCampaigns(
        user: FirebaseUser,
        onSuccess: (List<CampaignProfile>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("users").document(user.uid)
            .collection("campaigns")
            .get()
            .addOnSuccessListener { refs ->
                val profiles = refs.documents.mapNotNull { doc ->
                    val campaignId = doc.getString("campaignId") ?: doc.id
                    CampaignProfile(
                        id = campaignId,
                        name = doc.getString("name") ?: "Campaign",
                        moduleId = doc.getString("moduleId") ?: "sunless_citadel",
                        ownerUid = doc.getString("ownerUid"),
                        inviteCode = doc.getString("inviteCode"),
                        joined = doc.getBoolean("joined") ?: false
                    )
                }.sortedBy { it.name.lowercase() }
                onSuccess(profiles)
            }
            .addOnFailureListener {
                onFailure("Could not load your game worlds.")
            }
    }

    fun createCampaign(
        user: FirebaseUser,
        name: String,
        moduleId: String,
        onSuccess: (CampaignProfile) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val campaignRef = db.collection("campaigns").document()
        val campaignId = campaignRef.id
        val inviteCode = inviteCode()
        val profile = CampaignProfile(
            id = campaignId,
            name = name.ifBlank { "The Sunless Citadel" },
            moduleId = moduleId,
            ownerUid = user.uid,
            inviteCode = inviteCode,
            joined = false
        )

        val campaignData = hashMapOf<String, Any?>(
            "name" to profile.name,
            "moduleId" to moduleId,
            "ownerUid" to user.uid,
            "inviteCode" to inviteCode,
            "stateVersion" to 1L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val memberData = memberData(user, "owner")
        val pointerData = pointerData(profile)
        val inviteData = hashMapOf<String, Any?>(
            "campaignId" to campaignId,
            "ownerUid" to user.uid,
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.runBatch { batch ->
            batch.set(campaignRef, campaignData)
            batch.set(campaignRef.collection("members").document(user.uid), memberData)
            batch.set(
                db.collection("users").document(user.uid)
                    .collection("campaigns").document(campaignId),
                pointerData
            )
            batch.set(
                db.collection("inviteCodes").document(inviteCode),
                inviteData
            )
        }.addOnSuccessListener { onSuccess(profile) }
            .addOnFailureListener {
                onFailure("Could not create the game world.")
            }
    }
    fun joinCampaign(
        user: FirebaseUser,
        rawCode: String,
        onSuccess: (CampaignProfile) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val code = rawCode.trim().uppercase()
        if (code.length < 6) {
            onFailure("Enter the full invite code.")
            return
        }

        db.collection("inviteCodes").document(code).get()
            .addOnSuccessListener { invite ->
                val campaignId = invite.getString("campaignId")
                val active = invite.getBoolean("active") ?: false
                if (campaignId.isNullOrBlank() || !active) {
                    onFailure("That invite code is not active.")
                    return@addOnSuccessListener
                }
                loadCampaignForJoin(user, campaignId, onSuccess, onFailure)
            }
            .addOnFailureListener {
                onFailure("Invite code could not be checked.")
            }
    }
    private fun loadCampaignForJoin(
        user: FirebaseUser,
        campaignId: String,
        onSuccess: (CampaignProfile) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val campaignRef = db.collection("campaigns").document(campaignId)
        campaignRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                onFailure("The invited game world no longer exists.")
                return@addOnSuccessListener
            }
            val profile = CampaignProfile(
                id = doc.id,
                name = doc.getString("name") ?: "Campaign",
                moduleId = doc.getString("moduleId") ?: "sunless_citadel",
                ownerUid = doc.getString("ownerUid"),
                inviteCode = doc.getString("inviteCode"),
                joined = true
            )
            joinBatch(user, campaignRef, profile, onSuccess, onFailure)
        }.addOnFailureListener {
            onFailure("Could not open the invited game world.")
        }
    }

    private fun joinBatch(
        user: FirebaseUser,
        campaignRef: com.google.firebase.firestore.DocumentReference,
        profile: CampaignProfile,
        onSuccess: (CampaignProfile) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.runBatch { batch ->
            batch.set(
                campaignRef.collection("members").document(user.uid),
                memberData(user, "player")
            )
            batch.set(
                db.collection("users").document(user.uid)
                    .collection("campaigns").document(profile.id),
                pointerData(profile)
            )
        }.addOnSuccessListener { onSuccess(profile) }
            .addOnFailureListener {
                onFailure("Could not join the game world.")
            }
    }
    private fun memberData(user: FirebaseUser, role: String) =
        hashMapOf<String, Any?>(
            "role" to role,
            "displayName" to (user.displayName ?: "Player"),
            "email" to user.email,
            "joinedAt" to FieldValue.serverTimestamp()
        )

    private fun pointerData(profile: CampaignProfile) =
        hashMapOf<String, Any?>(
            "campaignId" to profile.id,
            "name" to profile.name,
            "moduleId" to profile.moduleId,
            "ownerUid" to profile.ownerUid,
            "inviteCode" to profile.inviteCode,
            "joined" to profile.joined,
            "updatedAt" to FieldValue.serverTimestamp()
        )

    private fun inviteCode(length: Int = 8): String =
        buildString {
            repeat(length) {
                append(alphabet[random.nextInt(alphabet.length)])
            }
        }
}
