package com.wayfarer.rpg

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class CampaignSyncRepository(
    private val campaignId: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private var eventsListener: ListenerRegistration? = null

    fun publishEvent(event: GameEvent) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val data = hashMapOf<String, Any?>(
            "title" to event.title,
            "body" to event.body,
            "type" to event.type,
            "timeLabel" to event.timeLabel,
            "userId" to user.uid,
            "displayName" to (user.displayName ?: "Player"),
            "createdAt" to FieldValue.serverTimestamp()
        )
        val started = System.nanoTime()
        val correlationId = LoreWiseDiagnostics.record(
            "sync", "publish_event", DiagnosticStatus.INFO
        )
        db.collection("campaigns").document(campaignId)
            .collection("events").document(event.id)
            .set(data)
            .addOnSuccessListener {
                LoreWiseDiagnostics.record(
                    "sync", "publish_event", DiagnosticStatus.OK,
                    durationMs = (System.nanoTime() - started) / 1_000_000,
                    correlationId = correlationId
                )
            }
            .addOnFailureListener { error ->
                LoreWiseDiagnostics.error(
                    "sync", "publish_event", error,
                    (System.nanoTime() - started) / 1_000_000,
                    correlationId
                )
            }
    }

    fun listenEvents(
        onEvents: (List<GameEvent>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        eventsListener?.remove()
        eventsListener = db.collection("campaigns").document(campaignId)
            .collection("events")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    LoreWiseDiagnostics.error(
                        "sync", "listen_events", error
                    )
                    onError("Campaign sync is temporarily unavailable.")
                    return@addSnapshotListener
                }
                val events = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    GameEvent(
                        title = title,
                        body = doc.getString("body") ?: "",
                        type = doc.getString("type") ?: "event",
                        timeLabel = doc.getString("timeLabel") ?: "Synced",
                        id = doc.id
                    )
                }
                LoreWiseDiagnostics.record(
                    "sync", "listen_events", DiagnosticStatus.OK,
                    detail = "count=${events.size}"
                )
                onEvents(events)
            }
    }

    fun close() {
        eventsListener?.remove()
        eventsListener = null
    }
}
