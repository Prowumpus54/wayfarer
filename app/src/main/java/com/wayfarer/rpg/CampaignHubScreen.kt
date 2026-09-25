package com.wayfarer.rpg

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun CampaignHubScreen(
    currentCampaign: CampaignProfile,
    onSelectCampaign: (CampaignProfile) -> Unit,
    onClose: () -> Unit
) {
    val activity = LocalActivity.current ?: return
    val auth = remember { FirebaseAuth.getInstance() }
    val cloud = remember { CampaignCloudRepository() }
    val google = remember { GoogleAccount(activity) }
    var user by remember { mutableStateOf(auth.currentUser) }
    var campaigns by remember { mutableStateOf<List<CampaignProfile>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("The Sunless Citadel") }
    var joinCode by remember { mutableStateOf("") }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            user = it.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
            google.close()
        }
    }

    fun refresh() {
        val activeUser = user ?: return
        busy = true
        cloud.loadCampaigns(
            activeUser,
            onSuccess = {
                campaigns = it
                busy = false
            },
            onFailure = {
                status = it
                busy = false
            }
        )
    }

    LaunchedEffect(user?.uid) {
        campaigns = emptyList()
        status = ""
        refresh()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .padding(top = 8.dp),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "GAME WORLDS",
                        color = Green,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Campaigns",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Text
                    )
                }
                Text(
                    "Close",
                    color = Green,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(8.dp)
                )
            }
        }

        if (user == null) {
            item {
                FramedCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Sign in to share worlds",
                        color = Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Google sign-in gives each player a stable identity for owned and invited campaigns.",
                        color = Muted
                    )
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(
                        "Sign in with Google",
                        Modifier.fillMaxWidth()
                    ) {
                        busy = true
                        status = ""
                        google.signIn(
                            activity.getString(R.string.default_web_client_id),
                            onSuccess = {
                                user = auth.currentUser
                                busy = false
                            },
                            onFailure = {
                                status = it
                                busy = false
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You can keep playing the current world offline without signing in.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            item {
                FramedCard(Modifier.fillMaxWidth()) {
                    Text(
                        user?.displayName ?: user?.email ?: "Signed in",
                        color = Text,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        user?.email ?: "Firebase account",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Current world: " + currentCampaign.name,
                        color = Green,
                        fontSize = 12.sp
                    )
                }
            }

            item {
                SectionTitle("Your Worlds")
                if (busy) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Green,
                        trackColor = CardAlt
                    )
                }
            }

            if (campaigns.isEmpty() && !busy) {
                item {
                    Text(
                        "No synced worlds yet. Create one below or join with an invite code.",
                        color = Muted
                    )
                }
            }

            campaigns.forEach { campaign ->
                item {
                    CampaignCloudCard(
                        campaign = campaign,
                        selected = campaign.id == currentCampaign.id,
                        onSelect = { onSelectCampaign(campaign) }
                    )
                }
            }
            item {
                FramedCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Create a World",
                        color = Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        label = { Text("Campaign name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(
                        "Create Sunless Citadel World",
                        Modifier.fillMaxWidth()
                    ) {
                        val activeUser = user ?: return@PrimaryButton
                        busy = true
                        status = ""
                        cloud.createCampaign(
                            activeUser,
                            createName,
                            "sunless_citadel",
                            onSuccess = {
                                campaigns = (campaigns + it)
                                    .distinctBy { profile -> profile.id }
                                busy = false
                                onSelectCampaign(it)
                            },
                            onFailure = {
                                status = it
                                busy = false
                            }
                        )
                    }
                }
            }

            item {
                FramedCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Join a World",
                        color = Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = {
                            joinCode = it.uppercase().take(8)
                        },
                        label = { Text("Invite code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(
                        "Join Campaign",
                        Modifier.fillMaxWidth()
                    ) {
                        val activeUser = user ?: return@PrimaryButton
                        busy = true
                        status = ""
                        cloud.joinCampaign(
                            activeUser,
                            joinCode,
                            onSuccess = {
                                campaigns = (campaigns + it)
                                    .distinctBy { profile -> profile.id }
                                joinCode = ""
                                busy = false
                                onSelectCampaign(it)
                            },
                            onFailure = {
                                status = it
                                busy = false
                            }
                        )
                    }
                }
            }

            item {
                TextButton(
                    onClick = {
                        auth.signOut()
                        campaigns = emptyList()
                    }
                ) {
                    Text("Sign out", color = Muted)
                }
            }
        }

        if (status.isNotBlank()) {
            item {
                Text(
                    status,
                    color = Gold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Card, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun CampaignCloudCard(
    campaign: CampaignProfile,
    selected: Boolean,
    onSelect: () -> Unit
) {
    FramedCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    campaign.name,
                    color = Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (campaign.joined) "Joined campaign" else "Owned campaign",
                    color = Muted,
                    fontSize = 12.sp
                )
                campaign.inviteCode?.let { code ->
                    Text(
                        "Invite: $code",
                        color = Green,
                        fontSize = 13.sp
                    )
                }
            }
            Text(
                if (selected) "ACTIVE" else "OPEN",
                color = if (selected) Green else Gold,
                fontSize = 11.sp
            )
        }
    }
}
