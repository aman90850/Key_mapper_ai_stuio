package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.service.FloatingOverlayService
import com.example.service.GamepadMapperAccessibilityService
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
fun BgmiOverlayDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        )
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    var isRunning by remember { mutableStateOf(FloatingOverlayService.isOverlayRunning) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("bgmi_overlay_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GamingSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonCyan)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0x3300E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PUBG / BGMI OVERLAY & TOUCH MAPPER",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Screen ke upar buttons chalane aur band karne ke liye",
                                color = TextSecondary,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Overlay Permission Card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GamingSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasOverlayPermission) NeonGreen.copy(alpha = 0.6f) else NeonAmber
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (hasOverlayPermission) NeonGreen else NeonAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (hasOverlayPermission) "1. Overlay Permission: ALLOWED" else "1. Overlay Permission Chahiye",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "PUBG ke upar transparent buttons dikhane ke liye",
                                    color = if (hasOverlayPermission) NeonGreen else TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        if (!hasOverlayPermission) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("ALLOW", color = Color.Black, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Accessibility Permission Card (Critical for in-game gamepad button simulation!)
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GamingSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasAccessibilityPermission) NeonGreen.copy(alpha = 0.6f) else NeonAmber
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (hasAccessibilityPermission) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = if (hasAccessibilityPermission) NeonGreen else NeonAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (hasAccessibilityPermission) "2. Touch Injection: ACTIVE" else "2. Gamepad Touch Service ON Karein",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "PUBG me physical Gamepad dabane par screen par touch fire karega",
                                    color = if (hasAccessibilityPermission) NeonGreen else TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        if (!hasAccessibilityPermission) {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("ENABLE", color = Color.Black, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Big Launch or Stop Overlay Button
                Button(
                    onClick = {
                        if (hasOverlayPermission) {
                            if (isRunning) {
                                FloatingOverlayService.stop(context)
                                isRunning = false
                            } else {
                                FloatingOverlayService.start(context)
                                isRunning = true
                                onDismiss()
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) NeonRed else NeonGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("launch_bgmi_overlay_btn")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.PowerSettingsNew else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "STOP / BAND KAREIN (EXIT OVERLAY)" else "START BGMI FLOATING OVERLAY",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Easy Steps
                Text(
                    text = "BAND KAISE KAREIN & GAME KAISE KHELEIN:",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                val steps = listOf(
                    "• BAND KARNA: Game ke upar top bar me laal rang ka [ EXIT ] button dabayein, ya notification drawer khol kar 'STOP OVERLAY' par click karein.",
                    "• BUTTONS SET KARNA: 'MOVE BUTTONS' par click karke Fire, Scope, Crouch, Prone ko game ke controls par rakhein.",
                    "• GAME KHELEIN: 'LOCKED (PLAY)' chunein — physical Gamepad buttons screen par directly tap/fire karenge!"
                )

                steps.forEach { step ->
                    Text(
                        text = step,
                        color = TextPrimary,
                        fontSize = 9.5.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D44)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                ) {
                    Text("OK, THEEK HAI", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    if (GamepadMapperAccessibilityService.isServiceRunning) return true
    return try {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        enabledServices.contains(context.packageName)
    } catch (e: Exception) {
        false
    }
}
