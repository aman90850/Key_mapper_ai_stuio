package com.example.input

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class AnalogStickState(
    val lx: Float = 0.0f,
    val ly: Float = 0.0f,
    val rx: Float = 0.0f,
    val ry: Float = 0.0f,
    val lt: Float = 0.0f,
    val rt: Float = 0.0f
)

data class InputEvent(
    val keyName: String,
    val keyCode: Int,
    val isDown: Boolean,
    val isGamepad: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

object InputEventBus {
    // Current pressed physical keys (e.g. "A", "SPACE", "RT", "W")
    private val _pressedKeys = MutableStateFlow<Set<String>>(emptySet())
    val pressedKeys = _pressedKeys.asStateFlow()

    // Analog stick coordinates
    private val _analogState = MutableStateFlow(AnalogStickState())
    val analogState = _analogState.asStateFlow()

    // Last input event for monitor display
    private val _lastInputEvent = MutableStateFlow<InputEvent?>(null)
    val lastInputEvent = _lastInputEvent.asStateFlow()

    // Broadcast stream for listeners
    private val _rawEvents = MutableSharedFlow<InputEvent>(extraBufferCapacity = 64)
    val rawEvents = _rawEvents.asSharedFlow()

    // Single-shot listener for "Listen for Input" modal
    var activeKeyCaptureListener: ((keyName: String, keyCode: Int, isGamepad: Boolean) -> Unit)? = null

    fun onKeyDown(keyName: String, keyCode: Int, isGamepad: Boolean): Boolean {
        val event = InputEvent(keyName = keyName, keyCode = keyCode, isDown = true, isGamepad = isGamepad)
        _lastInputEvent.value = event
        _pressedKeys.value = _pressedKeys.value + keyName
        _rawEvents.tryEmit(event)

        // Check if inspector is currently waiting for input
        val capture = activeKeyCaptureListener
        if (capture != null) {
            capture(keyName, keyCode, isGamepad)
            activeKeyCaptureListener = null
            return true
        }
        return false
    }

    fun onKeyUp(keyName: String, keyCode: Int, isGamepad: Boolean) {
        val event = InputEvent(keyName = keyName, keyCode = keyCode, isDown = false, isGamepad = isGamepad)
        _pressedKeys.value = _pressedKeys.value - keyName
        _rawEvents.tryEmit(event)
    }

    fun updateAnalog(lx: Float, ly: Float, rx: Float, ry: Float, lt: Float, rt: Float) {
        _analogState.value = AnalogStickState(
            lx = clampAxis(lx),
            ly = clampAxis(ly),
            rx = clampAxis(rx),
            ry = clampAxis(ry),
            lt = lt.coerceIn(0f, 1f),
            rt = rt.coerceIn(0f, 1f)
        )
    }

    private fun clampAxis(v: Float): Float {
        return if (kotlin.math.abs(v) < 0.05f) 0.0f else v.coerceIn(-1.0f, 1.0f)
    }
}
