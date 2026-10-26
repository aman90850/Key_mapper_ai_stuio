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
    RAPID_FIRE("Rapid Turbo Fire", "TURBO", "RT"),

    // Situational Vehicle Nodes (PUBG / BGMI Driving)
    VEHICLE_ACCEL("Drive / Accelerate (Gas)", "GAS", "RT"),
    VEHICLE_REVERSE("Reverse / Back", "REV", "LT"),
    VEHICLE_STEER_LEFT("Steer Left", "LEFT", "L-LEFT"),
    VEHICLE_STEER_RIGHT("Steer Right", "RIGHT", "L-RIGHT"),
    VEHICLE_BRAKE("Handbrake / Brake", "BRAKE", "B"),
    VEHICLE_BOOST("Nitro / Speed Boost", "BOOST", "A"),
    VEHICLE_EXIT("Exit Vehicle", "EXIT", "Y"),
    VEHICLE_HORN("Horn", "HORN", "R3"),
    VEHICLE_CHANGE_SEAT("Change Seat", "SEAT", "X"),
    VEHICLE_LEAN_DOWN("Bike Lean Down (Nose)", "DOWN", "LB"),
    VEHICLE_LEAN_UP("Bike Lean Up (Wheelie)", "UP", "RB"),
    ENTER_VEHICLE("Enter Vehicle / Drive", "DRIVE", "Y")
}
