package com.wayfarer.rpg

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private data class ModuleMapChoice(
    val id: String,
    val title: String,
    val asset: String
)

private val sunlessMaps = listOf(
    ModuleMapChoice(
        "overview",
        "Overview",
        "modules/sunless_citadel/maps/overview_maps.webp"
    ),
    ModuleMapChoice(
        "grove",
        "Grove Level",
        "modules/sunless_citadel/maps/grove_level_map.webp"
    )
)

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    campaignTitle: String,
    party: List<PartyMember>,
    currentLocation: ModuleLocation?,
    destinations: List<ModuleDestination>,
    discoveredLocationIds: Set<String>,
    onTravel: (ModuleDestination) -> Unit
) {
    var selectedId by remember(currentLocation?.id, destinations) {
        mutableStateOf(currentLocation?.id ?: destinations.firstOrNull()?.id)
    }
    var selectedMapId by remember { mutableStateOf("overview") }
    val selectedMap = sunlessMaps.first { it.id == selectedMapId }
    val selectedDestination = destinations.firstOrNull { it.id == selectedId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Bg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                campaignTitle.uppercase(),
                color = Green,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
            Text(
                "Module Map",
                style = MaterialTheme.typography.headlineMedium,
                color = Text
            )
            Text(
                "Canonical Sunless Citadel map art",
                color = Muted,
                fontSize = 12.sp
            )
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Surface)
            ) {
                sunlessMaps.forEach { choice ->
                    Text(
                        choice.title,
                        color = if (selectedMapId == choice.id) Text else Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMapId = choice.id }
                            .background(
                                if (selectedMapId == choice.id) GreenDark else Surface
                            )
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }

        item {
            Text(
                (currentLocation?.name ?: "Current location") +
                    " • " + discoveredLocationIds.size + " discovered",
                color = Muted,
                fontSize = 12.sp
            )
        }

        item {
            CanonicalMapViewer(
                assetPath = selectedMap.asset,
                title = selectedMap.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            )
        }

        item {
            FramedCard(Modifier.fillMaxWidth()) {
                Text(
                    if (selectedDestination == null) {
                        currentLocation?.name ?: "Current Location"
                    } else {
                        selectedDestination.name
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Text
                )

                Text(
                    if (selectedDestination == null) {
                        currentLocation?.playerDescription
                            ?.takeIf { it.isNotBlank() }
                            ?: "The party's current location."
                    } else {
                        selectedDestination.travelText.ifBlank {
                            "A route from the current location."
                        }
                    },
                    color = Muted
                )

                if (selectedDestination != null) {
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(
                        "Travel →",
                        Modifier.fillMaxWidth()
                    ) {
                        onTravel(selectedDestination)
                    }
                }
            }
        }

        if (destinations.isNotEmpty()) {
            item {
                SectionTitle("Nearby exits")
                Spacer(Modifier.height(6.dp))
                destinations.forEach { destination ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedId = destination.id }
                            .background(
                                if (destination.id == selectedId) {
                                    GreenDark.copy(alpha = .35f)
                                } else {
                                    Bg
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("→", color = Gold, modifier = Modifier.width(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text(destination.name, color = Text)
                            if (destination.travelText.isNotBlank()) {
                                Text(
                                    destination.travelText,
                                    color = Muted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanonicalMapViewer(
    assetPath: String,
    title: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(assetPath) {
        runCatching {
            context.assets.open(assetPath).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }

    var scale by remember(assetPath) { mutableFloatStateOf(1f) }
    var offsetX by remember(assetPath) { mutableFloatStateOf(0f) }
    var offsetY by remember(assetPath) { mutableFloatStateOf(0f) }

    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, GoldDark, RoundedCornerShape(14.dp))
            .background(CardAlt)
    ) {
        if (bitmap == null) {
            Text(
                "Map asset could not be loaded.",
                color = Danger,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = "$title map",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(assetPath) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offsetX += pan.x
                            offsetY += pan.y
                            if (scale == 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            )

            Text(
                "Pinch to zoom • drag to pan",
                color = Text,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Surface.copy(alpha = .88f))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            )

            if (scale > 1f) {
                Text(
                    "Reset",
                    color = Green,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Surface.copy(alpha = .9f))
                        .clickable {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
