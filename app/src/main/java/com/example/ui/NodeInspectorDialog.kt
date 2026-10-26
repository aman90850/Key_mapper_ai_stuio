package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MappingNode
import com.example.ui.theme.GamingBorder
import com.example.ui.theme.GamingSurface
import com.example.ui.theme.GamingSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NodeInspectorDialog(
    node: MappingNode,
    isListening: Boolean,
    onStartListening: () -> Unit,
    onCancelListening: () -> Unit,
    onUpdateNode: (MappingNode) -> Unit,
    onDeleteNode: () -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember(node.id) { mutableStateOf(node.label) }
    var radius by remember(node.id) { mutableFloatStateOf(node.radiusDp) }
    var selectedColor by remember(node.id) { mutableStateOf(node.colorHex) }

    val colorOptions = listOf(
        "#00E5FF", // Cyan
        "#FFB300", // Amber
        "#FF5252", // Red
        "#26A69A", // Teal / Crouch
        "#AB47BC", // Purple / Prone
        "#448AFF", // Blue
        "#ECEFF1"  // White
    )

    fun applyChanges() {
        onUpdateNode(
            node.copy(
                label = label,
                radiusDp = radius,
                colorHex = selectedColor
            )
        )
    }

    Dialog(onDismissRequest = {
        applyChanges()
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("node_inspector_dialog"),
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
                    Column {
                        Text(
                            text = "CUSTOMIZE BUTTON",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = node.label,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onDeleteNode,
                            modifier = Modifier.size(36.dp).testTag("delete_node_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed)
                        }
                        IconButton(
                            onClick = {
                                applyChanges()
                                onDismiss()
                            },
                            modifier = Modifier.size(36.dp).testTag("close_inspector_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rebind Card (Prominent "Press Button on Gamepad")
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GamingSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223149))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CURRENT BOUND KEY",
                            color = TextMuted,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "[ ${node.boundInput} ]",
                            color = NeonAmber,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isListening) {
                            val infiniteTransition = rememberInfiniteTransition(label = "listening_pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "listening_alpha"
                            )

                            Button(
                                onClick = onCancelListening,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = alpha)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("cancel_listen_button")
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PRESS BUTTON ON GAMEPAD...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onStartListening,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp).testTag("listen_for_input_button")
                            ) {
                                Icon(Icons.Default.Hearing, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PRESS ANY KEY TO REBIND", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Label TextField
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        applyChanges()
                    },
                    label = { Text("Button Name (e.g. Fire, Jump, Crouch)", color = TextSecondary, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GamingBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("node_label_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("BUTTON SIZE", color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                    Text("${radius.toInt()} dp", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = radius,
                    onValueChange = {
                        radius = it
                        applyChanges()
                    },
                    valueRange = 25f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = GamingBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("node_radius_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Color Selection
                Text(
                    text = "BUTTON COLOR",
                    color = TextMuted,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val isColorSelected = selectedColor.equals(hex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isColorSelected) 3.dp else 1.dp,
                                    color = if (isColorSelected) Color.White else GamingBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = hex
                                    applyChanges()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        applyChanges()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("save_inspector_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
