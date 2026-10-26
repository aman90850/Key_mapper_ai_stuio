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
import com.example.model.PlayerSituation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.hypot

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

    // Continuous Movement Engine State
    private var currentMovementStroke: GestureDescription.StrokeDescription? = null
    private var lastMovementTargetX = 0f
    private var lastMovementTargetY = 0f
    private var isMovementGestureInFlight = false
    private var isMovementLoopActive = false

    private val movementTickRunnable = object : Runnable {
        override fun run() {
            val shouldContinue = processMovementTick()
            if (shouldContinue || currentMovementStroke != null) {
                mainHandler.postDelayed(this, 40L)
            } else {
                isMovementLoopActive = false
            }
        }
    }

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
        Log.d(TAG, "GamepadMapperAccessibilityService connected for PUBG continuous movement")

        startInputMonitoring()
    }

    private fun startInputMonitoring() {
        // Continuous monitoring of Right Stick (Camera Aim), Left Stick (Movement), and Triggers
        serviceScope.launch {
            InputEventBus.analogState.collectLatest { state ->
                val rx = state.rx
                val ry = state.ry
                val now = SystemClock.uptimeMillis()

                // PUBG Right Stick Camera Aiming (Smooth swiping throttled to 40ms)
                if ((abs(rx) > 0.08f || abs(ry) > 0.08f) && (now - lastAimSwipeTime > 40L)) {
                    lastAimSwipeTime = now
                    val (screenW, screenH) = getRealScreenBounds()
                    val activeProfile = profileRepository.getActiveProfile()
                    val lookNode = activeProfile.nodes.firstOrNull {
                        it.nodeType == NodeType.JOYSTICK_RIGHT || it.label.contains("LOOK", ignoreCase = true)
                    }

                    val cameraCenterX = if (lookNode != null) (lookNode.xPercent * screenW) else (screenW * 0.76f)
                    val cameraCenterY = if (lookNode != null) (lookNode.yPercent * screenH) else (screenH * 0.58f)

                    val swipeDist = 75f
                    val endX = (cameraCenterX + (rx * swipeDist)).coerceIn(10f, screenW - 10f)
                    val endY = (cameraCenterY + (ry * swipeDist)).coerceIn(10f, screenH - 10f)
                    dispatchSwipe(cameraCenterX, cameraCenterY, endX, endY, 35L)
                }

                // Check Left Stick or trigger changes to wake up continuous movement engine
                if (abs(state.lx) > 0.12f || abs(state.ly) > 0.12f) {
                    wakeMovementEngine()
                }

                // Analog Trigger RT (Fire) support for gamepads
                if (state.rt > 0.35f) {
                    if (pressedKeyNames.add("RT")) {
                        handleKeyInitialPress("RT")
                    }
                } else if (state.rt < 0.20f) {
                    if (pressedKeyNames.remove("RT")) {
                        handleKeyRelease("RT")
                    }
                }

                // Analog Trigger LT (Scope) support for gamepads
                if (state.lt > 0.35f) {
                    if (pressedKeyNames.add("LT")) {
                        handleKeyInitialPress("LT")
                    }
                } else if (state.lt < 0.20f) {
                    if (pressedKeyNames.remove("LT")) {
                        handleKeyRelease("LT")
                    }
                }
            }
        }
    }

    private fun wakeMovementEngine() {
        if (!isMovementLoopActive) {
            isMovementLoopActive = true
            mainHandler.post(movementTickRunnable)
        }
    }

    /**
     * PUBG Genuine Movement Engine:
     * Maintains a continuous virtual thumb contact on the in-game movement joystick using
     * Android's willContinue and continueStroke APIs. The thumb stays down without lifting
     * every 50ms, eliminating all stutter and giving authentic, smooth character movement!
     */
    private fun processMovementTick(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val (screenW, screenH) = getRealScreenBounds()
        val analog = InputEventBus.analogState.value
        val stickX = analog.lx
        val stickY = analog.ly

        // Keyboard WASD & D-PAD input detection
        var kbX = 0f
        var kbY = 0f
        if (pressedKeyNames.contains("W") || pressedKeyNames.contains("D-PAD UP")) kbY -= 1f
        if (pressedKeyNames.contains("S") || pressedKeyNames.contains("D-PAD DOWN")) kbY += 1f
        if (pressedKeyNames.contains("A") || pressedKeyNames.contains("D-PAD LEFT")) kbX -= 1f
        if (pressedKeyNames.contains("D") || pressedKeyNames.contains("D-PAD RIGHT")) kbX += 1f

        if (kbX != 0f && kbY != 0f) {
            val diagonalFactor = 0.7071f
            kbX *= diagonalFactor
            kbY *= diagonalFactor
        }

        // Determine final movement vector
        val moveX: Float
        val moveY: Float
        if (abs(stickX) > 0.12f || abs(stickY) > 0.12f) {
            moveX = stickX
            moveY = stickY
        } else {
            moveX = kbX
            moveY = kbY
        }

        val magnitude = hypot(moveX, moveY)
        val isMoving = magnitude > 0.12f

        // Detect sprint request (SHIFT key, L3 button, or stick pushed far forward)
        val isSprinting = pressedKeyNames.contains("SHIFT") ||
                pressedKeyNames.contains("L3") ||
                pressedKeyNames.contains("SPRINT") ||
                moveY < -0.80f

        // Find the user's on-screen PUBG joystick placement from the active profile
        val activeProfile = profileRepository.getActiveProfile()
        val moveNode = activeProfile.nodes.firstOrNull {
            it.nodeType == NodeType.JOYSTICK_LEFT ||
                    it.label.contains("MOVE", ignoreCase = true) ||
                    it.label.contains("WASD", ignoreCase = true)
        }

        val joyCenterX = if (moveNode != null) (moveNode.xPercent * screenW).coerceIn(20f, screenW - 20f) else (screenW * 0.18f)
        val joyCenterY = if (moveNode != null) (moveNode.yPercent * screenH).coerceIn(20f, screenH - 20f) else (screenH * 0.68f)

        val baseRadius = if (moveNode != null && moveNode.radiusDp > 20f) {
            moveNode.radiusDp * 1.6f
        } else {
            80f
        }

        if (isMoving) {
            val dragDist = if (isSprinting) {
                // Pulls thumb higher into PUBG's top sprint-lock zone
                baseRadius * 1.55f
            } else {
                baseRadius * magnitude.coerceIn(0.6f, 1.0f)
            }

            val targetX = (joyCenterX + (moveX / magnitude.coerceAtLeast(1f)) * dragDist).coerceIn(10f, screenW - 10f)
            val targetY = (joyCenterY + (moveY / magnitude.coerceAtLeast(1f)) * dragDist).coerceIn(10f, screenH - 10f)

            if (isMovementGestureInFlight) {
                // A gesture is currently processing in the system; wait for callback
                return true
            }

            if (currentMovementStroke == null) {
                // Initial Touch: Place thumb down at joystick center and drag out to target
                val path = Path().apply {
                    moveTo(joyCenterX, joyCenterY)
                    lineTo(targetX, targetY)
                }
                val stroke = GestureDescription.StrokeDescription(path, 0L, 45L, true)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                currentMovementStroke = stroke
                lastMovementTargetX = targetX
                lastMovementTargetY = targetY
                isMovementGestureInFlight = true

                dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        isMovementGestureInFlight = false
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        isMovementGestureInFlight = false
                        currentMovementStroke = null
                    }
                }, null)
            } else {
                // Continuous Hold: Update thumb drag position without lifting finger
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val path = Path().apply {
                        moveTo(lastMovementTargetX, lastMovementTargetY)
                        lineTo(targetX, targetY)
                    }
                    try {
                        val nextStroke = currentMovementStroke!!.continueStroke(path, 0L, 45L, true)
                        val gesture = GestureDescription.Builder().addStroke(nextStroke).build()
                        currentMovementStroke = nextStroke
                        lastMovementTargetX = targetX
                        lastMovementTargetY = targetY
                        isMovementGestureInFlight = true

                        dispatchGesture(gesture, object : GestureResultCallback() {
                            override fun onCompleted(gestureDescription: GestureDescription?) {
                                isMovementGestureInFlight = false
                            }
                            override fun onCancelled(gestureDescription: GestureDescription?) {
                                isMovementGestureInFlight = false
                                currentMovementStroke = null
                            }
                        }, null)
                    } catch (e: Exception) {
                        isMovementGestureInFlight = false
                        currentMovementStroke = null
                    }
                }
            }
            return true
        } else {
            // Movement stopped: Cleanly lift the thumb so character stops immediately
            if (currentMovementStroke != null && !isMovementGestureInFlight) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val path = Path().apply {
                        moveTo(lastMovementTargetX, lastMovementTargetY)
                        lineTo(lastMovementTargetX, lastMovementTargetY)
                    }
                    try {
                        val endStroke = currentMovementStroke!!.continueStroke(path, 0L, 25L, false)
                        val gesture = GestureDescription.Builder().addStroke(endStroke).build()
                        isMovementGestureInFlight = true
                        dispatchGesture(gesture, object : GestureResultCallback() {
                            override fun onCompleted(gestureDescription: GestureDescription?) {
                                isMovementGestureInFlight = false
                                currentMovementStroke = null
                            }
                            override fun onCancelled(gestureDescription: GestureDescription?) {
                                isMovementGestureInFlight = false
                                currentMovementStroke = null
                            }
                        }, null)
                    } catch (e: Exception) {
                        isMovementGestureInFlight = false
                        currentMovementStroke = null
                    }
                } else {
                    currentMovementStroke = null
                }
            }
            return false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility interrupted")
        currentMovementStroke = null
        isMovementGestureInFlight = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllSprays()
        currentMovementStroke = null
        isMovementGestureInFlight = false
        mainHandler.removeCallbacks(movementTickRunnable)
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
            // Wake movement engine if movement keys are pressed
            if (keyName in setOf("W", "A", "S", "D", "SHIFT", "L3", "D-PAD UP", "D-PAD DOWN", "D-PAD LEFT", "D-PAD RIGHT")) {
                wakeMovementEngine()
            }

            val activeProfile = profileRepository.getActiveProfile()
            val matchedNodes = activeProfile.nodes.filter {
                it.boundInput.equals(keyName, ignoreCase = true) ||
                        (it.boundInput.equals("WASD", ignoreCase = true) && keyName in setOf("W", "A", "S", "D"))
            }

            if (matchedNodes.isEmpty()) return

            val (screenW, screenH) = getRealScreenBounds()

            for (node in matchedNodes) {
                // If it's the joystick node, movement engine handles it continuously
                if (node.nodeType == NodeType.JOYSTICK_LEFT) {
                    wakeMovementEngine()
                    continue
                }

                val targetX = (node.xPercent * screenW).coerceIn(10f, screenW - 10f)
                val targetY = (node.yPercent * screenH).coerceIn(10f, screenH - 10f)

                // 1. Continuous Auto-Fire for Guns in PUBG (Holding RT continues spraying)
                if (node.nodeType == NodeType.FIRE || node.nodeType == NodeType.RAPID_FIRE || keyName.equals("RT", ignoreCase = true) || node.label.contains("FIRE", ignoreCase = true)) {
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

        // Wake movement engine to detect stopped key
        if (keyName in setOf("W", "A", "S", "D", "SHIFT", "L3", "D-PAD UP", "D-PAD DOWN", "D-PAD LEFT", "D-PAD RIGHT")) {
            wakeMovementEngine()
        }

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

