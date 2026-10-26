package com.example.input

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.model.HardwareDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InputDetector(private val context: Context) : InputManager.InputDeviceListener {

    private val inputManager = try {
        context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    } catch (e: Exception) {
        null
    }

    private val _connectedDevices = MutableStateFlow<List<HardwareDevice>>(emptyList())
    val connectedDevices = _connectedDevices.asStateFlow()

    private val _activeDevice = MutableStateFlow<HardwareDevice?>(null)
    val activeDevice = _activeDevice.asStateFlow()

    private var lastHatX: Float = 0f
    private var lastHatY: Float = 0f

    fun startListening() {
        try {
            inputManager?.registerInputDeviceListener(this, null)
        } catch (e: Exception) {
            // Ignore if inputManager is unavailable
        }
        refreshDevices()
    }

    fun stopListening() {
        try {
            inputManager?.unregisterInputDeviceListener(this)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        refreshDevices()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        refreshDevices()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        refreshDevices()
    }

    fun refreshDevices() {
        try {
            val deviceIds = try {
                inputManager?.inputDeviceIds ?: InputDevice.getDeviceIds()
            } catch (e: Exception) {
                null
            } ?: intArrayOf()

            val list = mutableListOf<HardwareDevice>()

            for (id in deviceIds) {
                try {
                    val device = InputDevice.getDevice(id) ?: continue

                    val sources = device.sources
                    val hasGamepadSource = (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                            (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
                    val hasKeyboardSource = (sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD)

                    val isGamepad = hasGamepadSource || device.controllerNumber > 0 || device.name.contains("gamepad", ignoreCase = true) || device.name.contains("controller", ignoreCase = true) || device.name.contains("xbox", ignoreCase = true) || device.name.contains("playstation", ignoreCase = true)
                    val isKeyboard = hasKeyboardSource && (device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC)

                    if (isGamepad || isKeyboard || hasGamepadSource) {
                        val hwDevice = HardwareDevice(
                            id = id,
                            name = device.name ?: if (isGamepad) "Gamepad #$id" else "Keyboard #$id",
                            isGamepad = isGamepad,
                            isKeyboard = isKeyboard,
                            descriptor = device.descriptor ?: "",
                            isConnected = true
                        )
                        list.add(hwDevice)
                        if (_activeDevice.value == null && isGamepad) {
                            _activeDevice.value = hwDevice
                        }
                    }
                } catch (e: Exception) {
                    // Skip device on error
                }
            }

            if (list.isEmpty()) {
                val placeholder = HardwareDevice(
                    id = -1,
                    name = "Ready for Gamepad (Bluetooth / USB)",
                    isGamepad = true,
                    isKeyboard = false,
                    isConnected = false
                )
                list.add(placeholder)
            } else if (_activeDevice.value == null) {
                _activeDevice.value = list.firstOrNull { it.isConnected }
            }

            _connectedDevices.value = list
        } catch (e: Exception) {
            _connectedDevices.value = listOf(
                HardwareDevice(
                    id = -1,
                    name = "Ready for Gamepad (Bluetooth / USB)",
                    isGamepad = true,
                    isKeyboard = false,
                    isConnected = false
                )
            )
        }
    }

    private fun registerDeviceFromEvent(device: InputDevice?) {
        if (device == null) return
        try {
            val sources = device.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                    (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) ||
                    device.controllerNumber > 0 ||
                    device.name.contains("gamepad", ignoreCase = true) ||
                    device.name.contains("controller", ignoreCase = true) ||
                    device.name.contains("xbox", ignoreCase = true) ||
                    device.name.contains("playstation", ignoreCase = true)

            val hwDevice = HardwareDevice(
                id = device.id,
                name = device.name ?: if (isGamepad) "Gamepad Controller" else "Input Device",
                isGamepad = isGamepad,
                isKeyboard = (sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD),
                descriptor = device.descriptor ?: "",
                isConnected = true
            )
            _activeDevice.value = hwDevice

            // Update connected devices list
            val current = _connectedDevices.value.filter { it.id != -1 && it.id != device.id }.toMutableList()
            current.add(0, hwDevice)
            _connectedDevices.value = current
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        return try {
            val keyCode = event.keyCode
            val isGamepad = isGamepadEvent(event)
            val keyName = translateKeyCode(keyCode, isGamepad)

            // Register the device that fired this key event!
            try {
                val dev = event.device ?: InputDevice.getDevice(event.deviceId)
                if (dev != null) {
                    registerDeviceFromEvent(dev)
                }
            } catch (e: Exception) {
                // Ignore
            }

            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    return InputEventBus.onKeyDown(keyName, keyCode, isGamepad)
                }
            } else if (event.action == KeyEvent.ACTION_UP) {
                InputEventBus.onKeyUp(keyName, keyCode, isGamepad)
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        return try {
            // Register device that fired motion event
            try {
                val dev = event.device ?: InputDevice.getDevice(event.deviceId)
                if (dev != null) {
                    registerDeviceFromEvent(dev)
                }
            } catch (e: Exception) {
                // Ignore
            }

            if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
                (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            ) {
                val lx = event.getAxisValue(MotionEvent.AXIS_X)
                val ly = event.getAxisValue(MotionEvent.AXIS_Y)

                val rx = when {
                    event.getAxisValue(MotionEvent.AXIS_Z) != 0.0f -> event.getAxisValue(MotionEvent.AXIS_Z)
                    else -> event.getAxisValue(MotionEvent.AXIS_RX)
                }
                val ry = when {
                    event.getAxisValue(MotionEvent.AXIS_RZ) != 0.0f -> event.getAxisValue(MotionEvent.AXIS_RZ)
                    else -> event.getAxisValue(MotionEvent.AXIS_RY)
                }

                val lt = when {
                    event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > 0f -> event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
                    event.getAxisValue(MotionEvent.AXIS_BRAKE) > 0f -> event.getAxisValue(MotionEvent.AXIS_BRAKE)
                    else -> 0f
                }
                val rt = when {
                    event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > 0f -> event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
                    event.getAxisValue(MotionEvent.AXIS_GAS) > 0f -> event.getAxisValue(MotionEvent.AXIS_GAS)
                    else -> 0f
                }

                InputEventBus.updateAnalog(lx, ly, rx, ry, lt, rt)

                val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
                val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

                if (hatX != lastHatX) {
                    if (lastHatX == -1.0f) InputEventBus.onKeyUp("D-PAD LEFT", KeyEvent.KEYCODE_DPAD_LEFT, true)
                    else if (lastHatX == 1.0f) InputEventBus.onKeyUp("D-PAD RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT, true)

                    if (hatX == -1.0f) InputEventBus.onKeyDown("D-PAD LEFT", KeyEvent.KEYCODE_DPAD_LEFT, true)
                    else if (hatX == 1.0f) InputEventBus.onKeyDown("D-PAD RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT, true)
                    lastHatX = hatX
                }

                if (hatY != lastHatY) {
                    if (lastHatY == -1.0f) InputEventBus.onKeyUp("D-PAD UP", KeyEvent.KEYCODE_DPAD_UP, true)
                    else if (lastHatY == 1.0f) InputEventBus.onKeyUp("D-PAD DOWN", KeyEvent.KEYCODE_DPAD_DOWN, true)

                    if (hatY == -1.0f) InputEventBus.onKeyDown("D-PAD UP", KeyEvent.KEYCODE_DPAD_UP, true)
                    else if (hatY == 1.0f) InputEventBus.onKeyDown("D-PAD DOWN", KeyEvent.KEYCODE_DPAD_DOWN, true)
                    lastHatY = hatY
                }

                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isGamepadEvent(event: KeyEvent): Boolean {
        return try {
            val source = event.source
            if ((source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            ) {
                true
            } else {
                KeyEvent.isGamepadButton(event.keyCode)
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun translateKeyCode(keyCode: Int, isGamepad: Boolean = false): String {
            return when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_A -> "A"
                KeyEvent.KEYCODE_BUTTON_B -> "B"
                KeyEvent.KEYCODE_BUTTON_X -> "X"
                KeyEvent.KEYCODE_BUTTON_Y -> "Y"
                KeyEvent.KEYCODE_BUTTON_L1 -> "LB"
                KeyEvent.KEYCODE_BUTTON_R1 -> "RB"
                KeyEvent.KEYCODE_BUTTON_L2 -> "LT"
                KeyEvent.KEYCODE_BUTTON_R2 -> "RT"
                KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
                KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
                KeyEvent.KEYCODE_BUTTON_START -> "START"
                KeyEvent.KEYCODE_BUTTON_SELECT -> "SELECT"
                KeyEvent.KEYCODE_BUTTON_MODE -> "HOME"
                KeyEvent.KEYCODE_DPAD_UP -> "D-PAD UP"
                KeyEvent.KEYCODE_DPAD_DOWN -> "D-PAD DOWN"
                KeyEvent.KEYCODE_DPAD_LEFT -> "D-PAD LEFT"
                KeyEvent.KEYCODE_DPAD_RIGHT -> "D-PAD RIGHT"
                KeyEvent.KEYCODE_DPAD_CENTER -> "A"

                // Keyboard keys
                KeyEvent.KEYCODE_SPACE -> "SPACE"
                KeyEvent.KEYCODE_ENTER -> "ENTER"
                KeyEvent.KEYCODE_ESCAPE -> "ESC"
                KeyEvent.KEYCODE_TAB -> "TAB"
                KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> "SHIFT"
                KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> "CTRL"
                KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> "ALT"
                KeyEvent.KEYCODE_DEL -> "BACKSPACE"

                // Common gaming keys
                KeyEvent.KEYCODE_W -> "W"
                KeyEvent.KEYCODE_A -> "A"
                KeyEvent.KEYCODE_S -> "S"
                KeyEvent.KEYCODE_D -> "D"
                KeyEvent.KEYCODE_E -> "E"
                KeyEvent.KEYCODE_Q -> "Q"
                KeyEvent.KEYCODE_R -> "R"
                KeyEvent.KEYCODE_F -> "F"
                KeyEvent.KEYCODE_C -> "C"
                KeyEvent.KEYCODE_Z -> "Z"
                KeyEvent.KEYCODE_X -> "X"
                KeyEvent.KEYCODE_V -> "V"
                KeyEvent.KEYCODE_B -> "B"
                KeyEvent.KEYCODE_M -> "M"
                KeyEvent.KEYCODE_G -> "G"
                KeyEvent.KEYCODE_T -> "T"
                KeyEvent.KEYCODE_1 -> "1"
                KeyEvent.KEYCODE_2 -> "2"
                KeyEvent.KEYCODE_3 -> "3"
                KeyEvent.KEYCODE_4 -> "4"
                KeyEvent.KEYCODE_5 -> "5"
                KeyEvent.KEYCODE_6 -> "6"
                KeyEvent.KEYCODE_7 -> "7"
                KeyEvent.KEYCODE_8 -> "8"
                KeyEvent.KEYCODE_9 -> "9"
                KeyEvent.KEYCODE_0 -> "0"

                else -> {
                    try {
                        val label = KeyEvent.keyCodeToString(keyCode)
                        label.removePrefix("KEYCODE_")
                    } catch (e: Exception) {
                        "KEY_$keyCode"
                    }
                }
            }
        }
    }
}
