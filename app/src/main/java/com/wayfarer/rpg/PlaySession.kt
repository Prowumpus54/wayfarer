package com.wayfarer.rpg

import androidx.compose.runtime.mutableStateOf

class PlaySession {
    val input = mutableStateOf("")
    val lastAction = mutableStateOf<String?>(null)
    val check = mutableStateOf<CheckResult?>(null)
    val checkName = mutableStateOf("Check")
    val gmNarration = mutableStateOf<String?>(null)
    val gmStatus = mutableStateOf("Ready")
    val gmBusy = mutableStateOf(false)
    val failedAction = mutableStateOf<String?>(null)
    val selectedDie = mutableStateOf(20)
    val diceCountText = mutableStateOf("1")
    val manualModifierText = mutableStateOf("0")
    val manualRoll = mutableStateOf<DiceRollResult?>(null)
    val pendingRoll = mutableStateOf<String?>(null)
    val pendingCheck = mutableStateOf<GmCheckRequest?>(null)
    val pendingContext = mutableStateOf<GmContext?>(null)
    val gmDiceModifiers = mutableStateOf<List<GmDiceModifier>>(emptyList())
}
