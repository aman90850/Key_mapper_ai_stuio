package com.example.model

enum class NodeType(val displayName: String, val defaultLabel: String, val defaultKey: String) {
    STANDARD_TAP("Standard Tap", "Action", "A"),
    FIRE("Fire Button", "FIRE", "RT"),
    SCOPE("Scope / Aim Down Sight", "SCOPE", "LT"),
    JUMP("Jump Button", "JUMP", "A"),
    CROUCH("Crouch Button (Independent)", "CROUCH", "B"),
    PRONE("Prone Button (Independent)", "PRONE", "R3"),
    RELOAD("Reload Weapon", "RELOAD", "X"),
    PEEK_LEFT("Peek Left", "PEEK L", "LB"),
    PEEK_RIGHT("Peek Right", "PEEK R", "RB"),
    JOYSTICK_LEFT("Movement Stick (WASD / Left Stick)", "L-STICK", "L-STICK"),
    JOYSTICK_RIGHT("Aim / Camera Look Stick", "R-STICK", "R-STICK"),
    COMBO_MACRO("Combo Dual-Target Macro", "MACRO", "Y"),
    RAPID_FIRE("Rapid Turbo Fire", "TURBO", "RT")
}
