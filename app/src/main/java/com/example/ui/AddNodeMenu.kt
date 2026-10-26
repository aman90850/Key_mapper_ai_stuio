package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirlineSeatLegroomExtra
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.NodeType
import com.example.ui.theme.GamingBorder
import com.example.ui.theme.GamingSurface
import com.example.ui.theme.GamingSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class NodeSpawnOption(
    val type: NodeType,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val defaultKey: String,
    val badgeColor: Color
)

@Composable
fun AddNodeMenu(
    onSpawn: (NodeType) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        NodeSpawnOption(
            type = NodeType.FIRE,
            icon = Icons.Default.FlashOn,
            title = "Fire Button",
            subtitle = "Main primary fire trigger",
            defaultKey = "RT",
            badgeColor = NeonRed
        ),
        NodeSpawnOption(
            type = NodeType.SCOPE,
            icon = Icons.Default.CenterFocusStrong,
            title = "Hold-to-Scope",
            subtitle = "Aim down sights ADS toggle/hold",
            defaultKey = "LT",
            badgeColor = NeonCyan
        ),
        // DEDICATED INDEPENDENT CROUCH BUTTON
        NodeSpawnOption(
            type = NodeType.CROUCH,
            icon = Icons.Default.AirlineSeatReclineNormal,
            title = "Crouch (Independent)",
            subtitle = "Dedicated separated crouch stance",
            defaultKey = "B / C",
            badgeColor = Color(0xFF26A69A)
        ),
        // DEDICATED INDEPENDENT PRONE BUTTON
        NodeSpawnOption(
            type = NodeType.PRONE,
            icon = Icons.Default.AirlineSeatLegroomExtra,
            title = "Prone (Independent)",
            subtitle = "Dedicated separated prone stance",
            defaultKey = "R3 / Z",
            badgeColor = Color(0xFFAB47BC)
        ),
        NodeSpawnOption(
            type = NodeType.JUMP,
            icon = Icons.Default.DirectionsRun,
            title = "Jump Button",
            subtitle = "Jump / Vault obstacle action",
            defaultKey = "A / Space",
            badgeColor = NeonAmber
        ),
        NodeSpawnOption(
            type = NodeType.RELOAD,
            icon = Icons.Default.Refresh,
            title = "Reload Button",
            subtitle = "Weapon reload & swap",
            defaultKey = "X / R",
            badgeColor = Color(0xFF448AFF)
        ),
        NodeSpawnOption(
            type = NodeType.JOYSTICK_LEFT,
            icon = Icons.Default.Gamepad,
            title = "Left Analog Stick",
            subtitle = "WASD Movement Thumbstick",
            defaultKey = "L-STICK",
            badgeColor = NeonCyan
        ),
        NodeSpawnOption(
            type = NodeType.JOYSTICK_RIGHT,
            icon = Icons.Default.ControlCamera,
            title = "Right Analog Stick",
            subtitle = "Camera Look & Aim Controller",
            defaultKey = "R-STICK",
            badgeColor = Color(0xFF448AFF)
        ),
        NodeSpawnOption(
            type = NodeType.COMBO_MACRO,
            icon = Icons.Default.Bolt,
            title = "Combo / Macro Button",
            subtitle = "1 key triggers 2 coords with delay",
            defaultKey = "Y / LB",
            badgeColor = Color(0xFFFF3366)
        ),
        NodeSpawnOption(
            type = NodeType.RAPID_FIRE,
            icon = Icons.Default.TouchApp,
            title = "Rapid Turbo Fire",
            subtitle = "Continuous pulse firing stream",
            defaultKey = "RT",
            badgeColor = NeonRed
        ),
        NodeSpawnOption(
            type = NodeType.PEEK_LEFT,
            icon = Icons.Default.Visibility,
            title = "Peek Left",
            subtitle = "Lean left around cover",
            defaultKey = "LB / Q",
            badgeColor = NeonCyan
        ),
        NodeSpawnOption(
            type = NodeType.PEEK_RIGHT,
            icon = Icons.Default.Visibility,
            title = "Peek Right",
            subtitle = "Lean right around cover",
            defaultKey = "RB / E",
            badgeColor = NeonCyan
        ),
        NodeSpawnOption(
            type = NodeType.STANDARD_TAP,
            icon = Icons.Default.TouchApp,
            title = "Custom Tap Button",
            subtitle = "Single touch action node",
            defaultKey = "Custom",
            badgeColor = Color(0xFFECEFF1)
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("add_node_menu_dialog"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = GamingSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GamingBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SPAWN HUD BUTTON NODE",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Select a specialized button or controller to place",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(options) { item ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = GamingSurfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D44)),
                            modifier = Modifier
                                .clickable {
                                    onSpawn(item.type)
                                    onDismiss()
                                }
                                .testTag("spawn_option_${item.type.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(item.badgeColor.copy(alpha = 0.15f))
                                        .border(1.dp, item.badgeColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.badgeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = item.subtitle,
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "DEFAULT: [${item.defaultKey}]",
                                        color = item.badgeColor,
                                        fontSize = 8.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
