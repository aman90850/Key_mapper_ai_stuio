package com.example.model

enum class HudBackgroundType(val title: String, val subtitle: String) {
    TACTICAL_SHOOTER("BGMI Reference HUD", "In-game reference layout for Fire, Crouch, Prone, Scope & Backpack"),
    TRANSPARENT_OVERLAY("100% Transparent Over PUBG", "Zero background, buttons float directly over PUBG game"),
    CYBERPUNK_GRID("Alignment Grid", "Precision alignment grid with coordinate markers"),
    DARK_ARENA("Clean Dark Canvas", "Minimal distraction-free dark gaming backdrop")
}
