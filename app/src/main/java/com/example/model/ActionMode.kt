package com.example.model

enum class ActionMode(val label: String, val description: String) {
    NORMAL_TAP("Tap", "Single instant touch tap on button press"),
    HOLD("Hold", "Continuous touch held as long as key is pressed"),
    RAPID_TURBO("Turbo Fire", "Rapid pulsed taps while key is held down"),
    COMBO_MACRO("Combo Macro", "Triggers Primary then Secondary touch point with delay")
}
