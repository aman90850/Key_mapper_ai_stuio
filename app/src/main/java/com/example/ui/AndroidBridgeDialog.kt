package com.example.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bridge.OverlayBridge
import com.example.ui.theme.GamingBorder
import com.example.ui.theme.GamingSurface
import com.example.ui.theme.GamingSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AndroidBridgeDialog(
    overlayBridge: OverlayBridge,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasOverlay = overlayBridge.hasOverlayPermission()
    val hasAccessibility = overlayBridge.hasAccessibilityPermission()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("bridge_dialog"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GamingSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GamingBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsSystemDaydream, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "NATIVE ANDROID BRIDGE & OVERLAY",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "System permissions for floating overlay & touch simulation",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Permission Card 1: Draw Over Other Apps (SYSTEM_ALERT_WINDOW)
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = GamingSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (hasOverlay) NeonGreen.copy(alpha = 0.5f) else Color(0xFF263959))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (hasOverlay) Icons.Default.CheckCircle else Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (hasOverlay) NeonGreen else NeonAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Floating Window Overlay",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasOverlay) "Granted: Ready to float over games" else "Allows floating mapping HUD over full-screen games",
                                    color = if (hasOverlay) NeonGreen else TextMuted,
                                    fontSize = 9.5.sp
                                )
                            }
                        }

                        if (!hasOverlay) {
                            Button(
                                onClick = {
                                    val intent = overlayBridge.requestOverlayPermissionIntent()
                                    if (intent != null) context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("GRANT", color = Color.Black, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Permission Card 2: Accessibility Virtual Touch Injection
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = GamingSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (hasAccessibility) NeonGreen.copy(alpha = 0.5f) else Color(0xFF263959))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (hasAccessibility) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (hasAccessibility) NeonGreen else NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Virtual Touch Injection",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Low-latency software touch bridge for native APK games",
                                    color = TextMuted,
                                    fontSize = 9.5.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                context.startActivity(overlayBridge.requestAccessibilityIntent())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D44)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("SETTINGS", color = NeonCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware Compatibility Info
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33101B2E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2B44))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "SUPPORTED HARDWARE PROTOCOLS",
                            color = NeonAmber,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Xbox One / Series X|S / Elite Wireless (Bluetooth & USB)\n" +
                                   "• PlayStation DualShock 4 / DualSense PS5 (Bluetooth & USB)\n" +
                                   "• All USB OTG & 2.4GHz Dongle Gamepads & Keyboards\n" +
                                   "• Mechanical & Membrane Keyboards (WASD, Space, Shift, 1-9)",
                            color = TextSecondary,
                            fontSize = 9.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("OK, GOT IT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
