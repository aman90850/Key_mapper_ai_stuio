package com.example.model

data class InputBinding(
    val keyName: String,
    val keyCode: Int = 0,
    val isGamepad: Boolean = true,
    val isAnalog: Boolean = false,
    val axisCode: Int = -1
) {
    companion object {
        fun fromKey(name: String, code: Int = 0, isGamepad: Boolean = true): InputBinding {
            return InputBinding(
                keyName = name.uppercase(),
                keyCode = code,
                isGamepad = isGamepad
            )
        }
    }
}
