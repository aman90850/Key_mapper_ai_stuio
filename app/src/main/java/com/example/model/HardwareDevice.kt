package com.example.model

data class HardwareDevice(
    val id: Int,
    val name: String,
    val isGamepad: Boolean,
    val isKeyboard: Boolean,
    val descriptor: String = "",
    val isConnected: Boolean = true
)
