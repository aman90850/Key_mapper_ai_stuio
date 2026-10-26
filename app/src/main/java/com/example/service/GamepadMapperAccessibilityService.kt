package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.data.ProfileRepository
import com.example.input.InputDetector
import com.example.input.InputEventBus
import com.example.model.ActionMode
import com.example.model.MappingNode
import com.example.model.NodeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class GamepadMapperAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var profileRepository: ProfileRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // Tracks which physical keys are currently actively pressed down
    private val pressedKeyNames = ConcurrentHashMap.newKeySet<String>()

    // Continuous auto-fire runnable map
    private val activeSprayRunnables = ConcurrentHashMap<String, Runnable>()

    // Throttle timestamps for analog stick camera/movement to keep gestures responsive without freezing
    private var lastAimSwipeTime = 0L
    private var lastMoveSwipeTime = 0L

    companion object {
        private const val TAG = "GamepadAccessibility"
        var instance: GamepadMapperAccessibilityService? = null
        val isServiceRunning: Boolean get() = instance != null

        fun dispatchTap(x: Float, y: Float, durationMs: Long = 30L, onFinished: (() -> Unit)? = null) {
            val service = instance ?: run {
                onFinished?.invoke()
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    // Non-empty path with lineTo guarantees touch detection by Android touch framework
                    val path = Path().apply {
                        val validX = x.coerceAtLeast(1f)
                        val validY = y.coerceAtLeast(1f)
                        moveTo(validX, validY)
                        lineTo(validX, validY)
                    }
                    val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
                    val gesture = GestureDescription.Builder().addStroke(stroke).build()

                    service.dispatchGesture(gesture, object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            onFinished?.invoke()
                        }
                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            onFinished?.invoke()
                        }
                    }, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error dispatching tap at ($x, $y)", e)
                    onFinished?.invoke()
                }
            } else {
                onFinished?.invoke()
            }
        }

        fun dispatchSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 60L, onFinished: (() -> Unit)? = null) {
            val service = instance ?: run {
                onFinished?.invoke()
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val path = Path().apply {
                        moveTo(startX.coerceAtLeast(1f), startY.coerceAtLeast(1f))
                        lineTo(endX.coerceAtLeast(1f), endY.coerceAtLeast(1f))
                    }
                    val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
                    val gesture = GestureDescription.Builder().addStroke(stroke).build()

                    service.dispatchGesture(gesture, object : GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            onFinished?.invoke()
                        }
                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            onFinished?.invoke()
                        }
                    }, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error dispatching swipe", e)
                    onFinished?.invoke()
                }
            } else {
                onFinished?.invoke()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        profileRepository = ProfileRepository(this)
        Log.d(TAG, "GamepadMapperAccessibilityService connected for PUBG optimization")

        startAnalogStickMonitoring()
    }

    private fun startAnalogStickMonitoring() {
        // Continuous monitoring of Right Stick (Camera Look/Aim) and Left Stick (Movement)
        serviceScope.launch {
            InputEventBus.analogState.collectLatest { state ->
                val rx = state.rx
                val ry = state.ry
                val lx = state.lx
                val ly = state.ly
                val now = SystemClock.uptimeMillis()

                // PUBG Right Stick Camera Aiming (Threshold > 0.12f, throttled to 50ms)
                if ((abs(rx) > 0.12f || abs(ry) > 0.12f) && (now - lastAimSwipeTime > 50L)) {
                    lastAimSwipeTime = now
                    val (screenW, screenH) = getRealScreenBounds()
                    // Center of PUBG Right Camera Look Area
                    val cameraCenterX = screenW * 0.75f
                    val cameraCenterY = screenH * 0.55f

                    val swipeDist = 70f
                    val endX = (cameraCenterX + (rx * swipeDist)).coerceIn(10f, screenW - 10f)
                    val endY = (cameraCenterY + (ry * swipeDist)).coerceIn(10f, screenH - 10f)
                    dispatchSwipe(cameraCenterX, cameraCenterY, endX, endY, 45L)
                }

                // PUBG Left Stick Movement (WASD simulation, throttled to 60ms)
                if ((abs(lx) > 0.15f || abs(ly) > 0.15f) && (now - lastMoveSwipeTime > 60L)) {
                    lastMoveSwipeTime = now
                    val (screenW, screenH) = getRealScreenBounds()
                    // Center of PUBG Movement Joystick
                    val joyCenterX = screenW * 0.18f
                    val joyCenterY = screenH * 0.68f

                    val moveDist = 65f
                    val endX = (joyCenterX + (lx * moveDist)).coerceIn(10f, screenW - 10f)
                    val endY = (joyCenterY + (ly * moveDist)).coerceIn(10f, screenH - 10f)
                    dispatchSwipe(joyCenterX, joyCenterY, endX, endY, 50L)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllSprays()
        serviceScope.cancel()
        if (instance == this) {
            instance = null
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return try {
            val isGamepad = isGamepadEvent(event)
            val keyName = InputDetector.translateKeyCode(event.keyCode, isGamepad)

            if (event.action == KeyEvent.ACTION_DOWN) {
                val isFirstPress = pressedKeyNames.add(keyName)
                InputEventBus.onKeyDown(keyName, event.keyCode, isGamepad)

                if (isFirstPress) {
                    handleKeyInitialPress(keyName)
                }
            } else if (event.action == KeyEvent.ACTION_UP) {
                pressedKeyNames.remove(keyName)
                InputEventBus.onKeyUp(keyName, event.keyCode, isGamepad)
                handleKeyRelease(keyName)
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error in onKeyEvent", e)
            false
        }
    }

    private fun handleKeyInitialPress(keyName: String) {
        try {
            val activeProfile = profileRepository.getActiveProfile()
            val matchedNodes = activeProfile.nodes.filter {
                it.boundInput.equals(keyName, ignoreCase = true)
            }

            if (matchedNodes.isEmpty()) return

            val (screenW, screenH) = getRealScreenBounds()

            for (node in matchedNodes) {
                val targetX = (node.xPercent * screenW).coerceIn(10f, screenW - 10f)
                val targetY = (node.yPercent * screenH).coerceIn(10f, screenH - 10f)

                // 1. Continuous Auto-Fire for Guns in PUBG (Holding RT continues spraying)
                if (node.nodeType == NodeType.FIRE || node.nodeType == NodeType.RAPID_FIRE || keyName.equals("RT", ignoreCase = true)) {
                    startContinuousAutoSpray(keyName, targetX, targetY)
                } else {
                    // Standard, Crouch, Prone, Scope, Jump, Peek, Reload, Medkit, Grenade, Guns
                    executeSingleNodeTouch(node, targetX, targetY, screenW, screenH)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling initial press for $keyName", e)
        }
    }

    private fun handleKeyRelease(keyName: String) {
        // Stop any continuous fire loop for this key
        stopContinuousSpray(keyName)

        // Handle Hold-to-Scope & Hold-to-Peek release mechanics for PUBG
        try {
            val activeProfile = profileRepository.getActiveProfile()
            val matchedNodes = activeProfile.nodes.filter {
                it.boundInput.equals(keyName, ignoreCase = true)
            }

            val (screenW, screenH) = getRealScreenBounds()

            for (node in matchedNodes) {
                val targetX = (node.xPercent * screenW).coerceIn(10f, screenW - 10f)
                val targetY = (node.yPercent * screenH).coerceIn(10f, screenH - 10f)

                // If Scope or Peek was set to HOLD mode, releasing key taps again to unscope / unpeek
                if (node.actionMode == ActionMode.HOLD && (node.nodeType == NodeType.SCOPE || node.nodeType == NodeType.PEEK_LEFT || node.nodeType == NodeType.PEEK_RIGHT)) {
                    dispatchTap(targetX, targetY, 30L)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun startContinuousAutoSpray(keyName: String, targetX: Float, targetY: Float) {
        stopContinuousSpray(keyName)

        val sprayRunnable = object : Runnable {
            override fun run() {
                if (!pressedKeyNames.contains(keyName)) return
                // Paced sequential touch dispatch with callback ensures zero gesture queue congestion and no UI freeze!
                dispatchTap(targetX, targetY, 25L) {
                    if (pressedKeyNames.contains(keyName)) {
                        // 45ms pause + 25ms duration = ~70ms interval (14 bullets/second for M416 / AKM full-auto spray)
                        mainHandler.postDelayed(this, 45L)
                    }
                }
            }
        }

        activeSprayRunnables[keyName] = sprayRunnable
        mainHandler.post(sprayRunnable)
    }

    private fun stopContinuousSpray(keyName: String) {
        val runnable = activeSprayRunnables.remove(keyName)
        if (runnable != null) {
            mainHandler.removeCallbacks(runnable)
        }
    }

    private fun stopAllSprays() {
        for ((_, runnable) in activeSprayRunnables) {
            mainHandler.removeCallbacks(runnable)
        }
        activeSprayRunnables.clear()
        pressedKeyNames.clear()
    }

    private fun executeSingleNodeTouch(node: MappingNode, targetX: Float, targetY: Float, screenW: Float, screenH: Float) {
        when (node.actionMode) {
            ActionMode.NORMAL_TAP -> {
                dispatchTap(targetX, targetY, 30L)
            }
            ActionMode.RAPID_TURBO -> {
                dispatchTap(targetX, targetY, 25L) {
                    mainHandler.postDelayed({
                        dispatchTap(targetX, targetY, 25L) {
                            mainHandler.postDelayed({
                                dispatchTap(targetX, targetY, 25L)
                            }, 50L)
                        }
                    }, 50L)
                }
            }
            ActionMode.COMBO_MACRO -> {
                dispatchTap(targetX, targetY, 30L)
                val secondX = (node.macroSecondX * screenW).coerceIn(10f, screenW - 10f)
                val secondY = (node.macroSecondY * screenH).coerceIn(10f, screenH - 10f)
                mainHandler.postDelayed({
                    dispatchTap(secondX, secondY, 30L)
                }, node.macroDelayMs.coerceIn(40L, 500L))
            }
            ActionMode.HOLD -> {
                dispatchTap(targetX, targetY, 30L)
            }
        }
    }

    private fun getRealScreenBounds(): Pair<Float, Float> {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width().toFloat(), bounds.height().toFloat())
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            Pair(metrics.widthPixels.toFloat(), metrics.heightPixels.toFloat())
        }
    }

    private fun isGamepadEvent(event: KeyEvent): Boolean {
        val source = event.source
        return (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
                KeyEvent.isGamepadButton(event.keyCode)
    }
}
