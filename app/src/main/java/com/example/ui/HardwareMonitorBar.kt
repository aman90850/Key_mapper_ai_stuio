package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.input.AnalogStickState
import com.example.input.InputEvent
import com.example.model.HardwareDevice
import com.example.ui.theme.GamingBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun HardwareMonitorBar(
    devices: List<HardwareDevice>,
    activeDevice: HardwareDevice?,
    lastEvent: InputEvent?,
    analogState: AnalogStickState,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = activeDevice != null && activeDevice.isConnected && activeDevice.id != -1
    val deviceName = if (isConnected) activeDevice!!.name else "No Gamepad Detected"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xE60A0E17))
            .border(1.dp, GamingBorder, RoundedCornerShape(0.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Connected Gamepad Status & Quick Scan
        Row(verticalAlignment = Alignment.CenterVertically) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) NeonGreen.copy(alpha = pulseAlpha) else NeonAmber
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                tint = if (isConnected) NeonGreen else NeonAmber,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Text(
                    text = if (isConnected) deviceName else "Connect via Bluetooth/USB & press any button",
                    color = TextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (isConnected) "GAMEPAD READY" else "Press any button on controller to detect",
                    color = if (isConnected) NeonGreen else TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D44)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(30.dp).testTag("scan_gamepads_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SCAN", color = NeonCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Section 2: Live Pressed Key Test Box
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "BUTTON TEST:",
                color = TextMuted,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))

            val keyText = lastEvent?.keyName ?: "None"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (lastEvent != null) Color(0x3300E5FF) else Color(0x22182338))
                    .border(
                        1.dp,
                        if (lastEvent != null) NeonCyan else GamingBorder,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "[$keyText]",
                    color = if (lastEvent != null) NeonCyan else TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Section 3: Left & Right Stick coordinates
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SimpleStickPreview(label = "L-STICK", x = analogState.lx, y = analogState.ly, color = NeonCyan)
            SimpleStickPreview(label = "R-STICK", x = analogState.rx, y = analogState.ry, color = NeonAmber)
        }
    }
}

@Composable
private fun SimpleStickPreview(
    label: String,
    x: Float,
    y: Float,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val r = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f

            drawCircle(color = Color(0xFF24334F), radius = r - 1f, style = Stroke(1.5f))
            val dotX = cx + (x.coerceIn(-1f, 1f) * (r - 3f))
            val dotY = cy + (y.coerceIn(-1f, 1f) * (r - 3f))
            drawCircle(color = color, radius = 3.5f, center = Offset(dotX, dotY))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format(Locale.US, "%s (%+.1f, %+.1f)", label, x, y),
            color = TextMuted,
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
