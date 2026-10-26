package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.HudBackgroundType
import com.example.ui.theme.GamingDarkBackground

@Composable
fun TacticalShooterHudBackground(
    backgroundType: HudBackgroundType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        if (backgroundType != HudBackgroundType.TRANSPARENT_OVERLAY) {
            drawRect(color = GamingDarkBackground)
        }

        when (backgroundType) {
            HudBackgroundType.CYBERPUNK_GRID -> {
                drawCyberpunkGrid(w, h)
            }
            HudBackgroundType.TACTICAL_SHOOTER -> {
                drawBgmiShooterTemplate(w, h)
            }
            HudBackgroundType.DARK_ARENA -> {
                drawDarkArena(w, h)
            }
            HudBackgroundType.TRANSPARENT_OVERLAY -> {
                // 100% transparent so PUBG/BGMI underneath is completely visible!
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBgmiShooterTemplate(w: Float, h: Float) {
    val outlineColor = Color(0xFF223552)
    val guideColor = Color(0xFF385580)
    val faintColor = Color(0xFF131D2D)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

    // Center Crosshair Reticle
    val cx = w * 0.5f
    val cy = h * 0.5f
    drawCircle(color = faintColor, radius = 24f, center = Offset(cx, cy), style = Stroke(width = 1.5f))
    drawCircle(color = guideColor, radius = 4f, center = Offset(cx, cy))
    drawLine(guideColor, Offset(cx - 20f, cy), Offset(cx - 8f, cy), 1.5f)
    drawLine(guideColor, Offset(cx + 8f, cy), Offset(cx + 20f, cy), 1.5f)
    drawLine(guideColor, Offset(cx, cy - 20f), Offset(cx, cy - 8f), 1.5f)
    drawLine(guideColor, Offset(cx, cy + 8f), Offset(cx, cy + 20f), 1.5f)

    // 1. BGMI FIRE BUTTON GUIDE (Right, around 0.88w, 0.35h)
    val fireCenter = Offset(w * 0.88f, h * 0.35f)
    drawCircle(color = Color(0x33FF5252), radius = 44f, center = fireCenter)
    drawCircle(color = Color(0x80FF5252), radius = 44f, center = fireCenter, style = Stroke(2f, pathEffect = dashEffect))

    // 2. BGMI SCOPE (ADS) GUIDE (Top Right, around 0.82w, 0.18h)
    val scopeCenter = Offset(w * 0.82f, h * 0.18f)
    drawCircle(color = Color(0x3300E5FF), radius = 40f, center = scopeCenter)
    drawCircle(color = Color(0x8000E5FF), radius = 40f, center = scopeCenter, style = Stroke(2f, pathEffect = dashEffect))

    // 3. BGMI JUMP BUTTON GUIDE (Right Center, around 0.92w, 0.60h)
    val jumpCenter = Offset(w * 0.92f, h * 0.60f)
    drawCircle(color = Color(0x33FFB300), radius = 38f, center = jumpCenter)
    drawCircle(color = Color(0x80FFB300), radius = 38f, center = jumpCenter, style = Stroke(2f, pathEffect = dashEffect))

    // 4. DEDICATED SEPARATED BGMI CROUCH GUIDE (Bottom Right, around 0.88w, 0.75h)
    val crouchCenter = Offset(w * 0.88f, h * 0.75f)
    drawCircle(color = Color(0x3326A69A), radius = 38f, center = crouchCenter)
    drawCircle(color = Color(0x8026A69A), radius = 38f, center = crouchCenter, style = Stroke(2f, pathEffect = dashEffect))

    // 5. DEDICATED SEPARATED BGMI PRONE GUIDE (Bottom Right Corner, around 0.78w, 0.86h)
    val proneCenter = Offset(w * 0.78f, h * 0.86f)
    drawCircle(color = Color(0x33AB47BC), radius = 38f, center = proneCenter)
    drawCircle(color = Color(0x80AB47BC), radius = 38f, center = proneCenter, style = Stroke(2f, pathEffect = dashEffect))

    // 6. BGMI RELOAD GUIDE (around 0.72w, 0.36h)
    val reloadCenter = Offset(w * 0.72f, h * 0.36f)
    drawCircle(color = Color(0x33448AFF), radius = 36f, center = reloadCenter)
    drawCircle(color = Color(0x80448AFF), radius = 36f, center = reloadCenter, style = Stroke(2f, pathEffect = dashEffect))

    // 7. BGMI MOVEMENT JOYSTICK GUIDE (Bottom Left, around 0.18w, 0.68h)
    val joyCenter = Offset(w * 0.18f, h * 0.68f)
    drawCircle(color = Color(0x2200E5FF), radius = 64f, center = joyCenter)
    drawCircle(color = Color(0x8000E5FF), radius = 64f, center = joyCenter, style = Stroke(2f, pathEffect = dashEffect))
    drawCircle(color = Color(0x4400E5FF), radius = 24f, center = joyCenter, style = Stroke(1.5f))

    // 8. BGMI PEEK GUIDES (0.15w, 0.35h & 0.24w, 0.35h)
    drawCircle(color = faintColor, radius = 30f, center = Offset(w * 0.15f, h * 0.35f))
    drawCircle(color = guideColor, radius = 30f, center = Offset(w * 0.15f, h * 0.35f), style = Stroke(1.5f, pathEffect = dashEffect))
    drawCircle(color = faintColor, radius = 30f, center = Offset(w * 0.24f, h * 0.35f))
    drawCircle(color = guideColor, radius = 30f, center = Offset(w * 0.24f, h * 0.35f), style = Stroke(1.5f, pathEffect = dashEffect))

    // Mini-Map Wireframe (Top Right)
    val mapRadius = h * 0.16f
    val mapCenter = Offset(w * 0.88f, h * 0.20f)
    drawCircle(color = outlineColor, radius = mapRadius, center = mapCenter, style = Stroke(width = 2f))

    // Bottom Center: Health / Weapon Bars
    val barW = w * 0.28f
    val barH = 12f
    val barX = (w - barW) / 2f
    val barY = h * 0.92f
    drawRect(color = faintColor, topLeft = Offset(barX, barY), size = Size(barW, barH))
    drawRect(color = outlineColor, topLeft = Offset(barX, barY), size = Size(barW, barH), style = Stroke(1.5f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCyberpunkGrid(w: Float, h: Float) {
    val gridColor = Color(0xFF131D2E)
    val majorColor = Color(0xFF1D2E47)
    val step = 48f

    var x = 0f
    var countX = 0
    while (x <= w) {
        val color = if (countX % 4 == 0) majorColor else gridColor
        drawLine(color, Offset(x, 0f), Offset(x, h), 1f)
        x += step
        countX++
    }

    var y = 0f
    var countY = 0
    while (y <= h) {
        val color = if (countY % 4 == 0) majorColor else gridColor
        drawLine(color, Offset(0f, y), Offset(w, y), 1f)
        y += step
        countY++
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDarkArena(w: Float, h: Float) {
    drawRect(color = Color(0xFF070B12))
    val cx = w * 0.5f
    val cy = h * 0.5f
    val r = kotlin.math.min(w, h) * 0.45f
    drawCircle(color = Color(0xFF101929), radius = r, center = Offset(cx, cy), style = Stroke(2f))
}
