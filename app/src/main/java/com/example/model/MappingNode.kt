package com.example.model

import java.util.UUID

data class MappingNode(
    val id: String = UUID.randomUUID().toString(),
    val nodeType: NodeType = NodeType.STANDARD_TAP,
    val label: String = "BTN",
    val boundInput: String = "A",
    val keyCode: Int = 0,
    val xPercent: Float = 0.5f,
    val yPercent: Float = 0.5f,
    val radiusDp: Float = 36f,
    val opacity: Float = 0.85f,
    val colorHex: String = "#00E5FF",
    val actionMode: ActionMode = ActionMode.NORMAL_TAP,
    val stickDeadzone: Float = 0.15f,
    val stickSensitivity: Float = 1.0f,
    val macroSecondX: Float = 0.55f,
    val macroSecondY: Float = 0.55f,
    val macroDelayMs: Long = 120L,
    val turboRateHz: Int = 12
)
