package com.example.bridge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

interface VirtualTouchInjector {
    fun injectDown(x: Float, y: Float, pointerId: Int = 0)
    fun injectMove(x: Float, y: Float, pointerId: Int = 0)
    fun injectUp(pointerId: Int = 0)
    fun injectTap(x: Float, y: Float, durationMs: Long = 50L)
    fun injectComboMacro(x1: Float, y1: Float, x2: Float, y2: Float, delayMs: Long = 100L)
}

class OverlayBridge(private val context: Context) : VirtualTouchInjector {

    companion object {
        private const val TAG = "OverlayBridge"
    }

    var isVirtualTouchActive: Boolean = true

    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestOverlayPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasOverlayPermission()) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else null
    }

    fun hasAccessibilityPermission(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(context.packageName)
    }

    fun requestAccessibilityIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    override fun injectDown(x: Float, y: Float, pointerId: Int) {
        if (!isVirtualTouchActive) return
        Log.d(TAG, "Touch INJECT DOWN at ($x, $y), pointer=$pointerId")
        com.example.service.GamepadMapperAccessibilityService.dispatchTap(x, y, 40L)
    }

    override fun injectMove(x: Float, y: Float, pointerId: Int) {
        if (!isVirtualTouchActive) return
        Log.d(TAG, "Touch INJECT MOVE at ($x, $y), pointer=$pointerId")
        com.example.service.GamepadMapperAccessibilityService.dispatchSwipe(x, y, x + 5f, y + 5f, 50L)
    }

    override fun injectUp(pointerId: Int) {
        if (!isVirtualTouchActive) return
        Log.d(TAG, "Touch INJECT UP, pointer=$pointerId")
    }

    override fun injectTap(x: Float, y: Float, durationMs: Long) {
        if (!isVirtualTouchActive) return
        Log.d(TAG, "Touch INJECT TAP at ($x, $y) for ${durationMs}ms")
        com.example.service.GamepadMapperAccessibilityService.dispatchTap(x, y, durationMs)
    }

    override fun injectComboMacro(x1: Float, y1: Float, x2: Float, y2: Float, delayMs: Long) {
        if (!isVirtualTouchActive) return
        Log.d(TAG, "Touch INJECT COMBO MACRO P1($x1, $y1) -> P2($x2, $y2) delay=${delayMs}ms")
        com.example.service.GamepadMapperAccessibilityService.dispatchTap(x1, y1, 40L)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            com.example.service.GamepadMapperAccessibilityService.dispatchTap(x2, y2, 40L)
        }, delayMs)
    }
}
