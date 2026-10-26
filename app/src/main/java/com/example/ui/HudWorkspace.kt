package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AirlineSeatLegroomExtra
import androidx.compose.material.icons.filled.AirlineSeatReclineExtra
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.input.AnalogStickState
import com.example.model.ActionMode
import com.example.model.HudBackgroundType
import com.example.model.MappingNode
import com.example.model.NodeType
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HudWorkspace(
    nodes: List<MappingNode>,
    isEditMode: Boolean,
    selectedNode: MappingNode?,
    pressedKeys: Set<String>,
    analogState: AnalogStickState,
    hudBackground: HudBackgroundType,
    onSelectNode: (MappingNode?) -> Unit,
    onUpdateNodePosition: (nodeId: String, xPercent: Float, yPercent: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTransparent = hudBackground == HudBackgroundType.TRANSPARENT_OVERLAY

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (isTransparent) Color.Transparent else Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val targetAspect = 16f / 9f
        val currentAspect = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else targetAspect

        val hudModifier = if (isTransparent) {
            // Match the device's native fullscreen dimensions exactly for 1:1 touch coordinate accuracy
            Modifier.fillMaxSize()
        } else if (currentAspect > targetAspect) {
            Modifier.aspectRatio(targetAspect, matchHeightConstraintsFirst = true)
        } else {
            Modifier.aspectRatio(targetAspect, matchHeightConstraintsFirst = false)
        }

        BoxWithConstraints(
            modifier = hudModifier
                .clip(RoundedCornerShape(0.dp))
                .border(
                    if (isEditMode && !isTransparent) 2.dp else 0.dp,
                    if (isEditMode) NeonAmber else Color.Transparent
                )
        ) {
            val workspaceWidth = constraints.maxWidth.toFloat()
            val workspaceHeight = constraints.maxHeight.toFloat()

            // Background Reference Canvas (Tactical Shooter / Cyber Grid / Dark Arena)
            TacticalShooterHudBackground(backgroundType = hudBackground)

            // Canvas for Macro lines
            MacroConnectorCanvas(nodes = nodes)

            // Render all mapping nodes
            nodes.forEach { node ->
                val isSelected = selectedNode?.id == node.id
                val isKeyPressed = pressedKeys.contains(node.boundInput.uppercase()) ||
                        (node.boundInput.equals("WASD", ignoreCase = true) &&
                                (pressedKeys.contains("W") || pressedKeys.contains("A") || pressedKeys.contains("S") || pressedKeys.contains("D")))

                HudNodeItem(
                    node = node,
                    workspaceWidth = workspaceWidth,
                    workspaceHeight = workspaceHeight,
                    isEditMode = isEditMode,
                    isSelected = isSelected,
                    isKeyPressed = isKeyPressed,
                    analogState = analogState,
                    onSelect = { onSelectNode(node) },
                    onPositionChanged = { newX, newY ->
                        onUpdateNodePosition(node.id, newX, newY)
                    }
                )
            }

            // Edit Mode Hint Banner
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xD9101827))
                        .border(1.dp, Color(0x66FFB300), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TOUCH & DRAG ANY BUTTON TO MOVE • TAP ⚙️ TO REBIND KEY",
                        color = NeonAmber,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroConnectorCanvas(nodes: List<MappingNode>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        nodes.filter { it.actionMode == ActionMode.COMBO_MACRO }.forEach { node ->
            val p1 = Offset(node.xPercent * w, node.yPercent * h)
            val p2 = Offset(node.macroSecondX * w, node.macroSecondY * h)

            drawLine(
                color = Color(0x99FF3366),
                start = p1,
                end = p2,
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            drawCircle(
                color = Color(0x66FF3366),
                radius = 14f,
                center = p2
            )
            drawCircle(
                color = Color(0xFFFF3366),
                radius = 14f,
                center = p2,
                style = Stroke(2f)
            )
        }
    }
}

@Composable
private fun HudNodeItem(
    node: MappingNode,
    workspaceWidth: Float,
    workspaceHeight: Float,
    isEditMode: Boolean,
    isSelected: Boolean,
    isKeyPressed: Boolean,
    analogState: AnalogStickState,
    onSelect: () -> Unit,
    onPositionChanged: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val nodeColor = remember(node.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(node.colorHex))
        } catch (e: Exception) {
            NeonCyan
        }
    }

    // Hit ripple animation trigger for physical key press
    val rippleScale = remember { Animatable(1.0f) }
    val rippleAlpha = remember { Animatable(0.0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isKeyPressed) {
        if (isKeyPressed) {
            coroutineScope.launch {
                rippleAlpha.snapTo(0.8f)
                rippleScale.snapTo(1.0f)
                rippleScale.animateTo(1.4f, animationSpec = tween(160))
                rippleAlpha.animateTo(0.0f, animationSpec = tween(160))
            }
        }
    }

    // Keep updated state references so dragging never uses stale coordinates
    val currentXPercent by rememberUpdatedState(node.xPercent)
    val currentYPercent by rememberUpdatedState(node.yPercent)
    val currentOnPositionChanged by rememberUpdatedState(onPositionChanged)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val diameterDp = (node.radiusDp * 2).dp
    val radiusPx = with(density) { node.radiusDp.dp.toPx() }

    val centerXPx = currentXPercent * workspaceWidth
    val centerYPx = currentYPercent * workspaceHeight

    val offsetX = (centerXPx - radiusPx).roundToInt()
    val offsetY = (centerYPx - radiusPx).roundToInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(diameterDp)
            .pointerInput(node.id, isEditMode) {
                if (isEditMode) {
                    detectDragGestures(
                        onDragStart = {
                            currentOnSelect()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (workspaceWidth > 0f && workspaceHeight > 0f) {
                                val deltaX = dragAmount.x / workspaceWidth
                                val deltaY = dragAmount.y / workspaceHeight
                                val newX = (currentXPercent + deltaX).coerceIn(0.04f, 0.96f)
                                val newY = (currentYPercent + deltaY).coerceIn(0.04f, 0.96f)
                                currentOnPositionChanged(newX, newY)
                            }
                        }
                    )
                }
            }
            .pointerInput(node.id, isEditMode) {
                if (isEditMode) {
                    detectTapGestures(
                        onTap = { currentOnSelect() }
                    )
                }
            }
            .testTag("node_${node.id}")
    ) {
        // Ripple on physical key hit
        if (rippleAlpha.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = nodeColor.copy(alpha = rippleAlpha.value),
                    radius = (size.minDimension / 2f) * rippleScale.value,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Node Inner View
        when (node.nodeType) {
            NodeType.JOYSTICK_LEFT, NodeType.JOYSTICK_RIGHT -> {
                JoystickNodeContent(
                    node = node,
                    nodeColor = nodeColor,
                    isEditMode = isEditMode,
                    isSelected = isSelected,
                    isKeyPressed = isKeyPressed,
                    analogState = analogState
                )
            }
            else -> {
                StandardButtonNodeContent(
                    node = node,
                    nodeColor = nodeColor,
                    isEditMode = isEditMode,
                    isSelected = isSelected,
                    isKeyPressed = isKeyPressed
                )
            }
        }

        // Small Gear Icon when selected in Edit mode
        if (isEditMode && isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(NeonAmber)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Edit settings",
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun StandardButtonNodeContent(
    node: MappingNode,
    nodeColor: Color,
    isEditMode: Boolean,
    isSelected: Boolean,
    isKeyPressed: Boolean
) {
    val activeAlpha = if (isKeyPressed) 1.0f else node.opacity
    val glowColor = if (isKeyPressed) Color.White else nodeColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                if (isKeyPressed) nodeColor.copy(alpha = 0.6f)
                else Color(0x330A101D).copy(alpha = activeAlpha * 0.45f)
            )
            .border(
                width = if (isSelected) 3.dp else if (isKeyPressed) 2.5.dp else 1.5.dp,
                color = if (isSelected) NeonAmber else glowColor.copy(alpha = activeAlpha),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(2.dp)
        ) {
            val icon = when (node.nodeType) {
                NodeType.FIRE -> Icons.Default.FlashOn
                NodeType.SCOPE -> Icons.Default.CenterFocusStrong
                NodeType.CROUCH -> Icons.Default.AirlineSeatReclineNormal
                NodeType.PRONE -> Icons.Default.AirlineSeatLegroomExtra
                NodeType.JUMP -> Icons.AutoMirrored.Filled.DirectionsRun
                NodeType.RELOAD -> Icons.Default.Refresh
                NodeType.PEEK_LEFT, NodeType.PEEK_RIGHT -> Icons.Default.Visibility
                NodeType.COMBO_MACRO -> Icons.Default.Bolt
                NodeType.RAPID_FIRE -> Icons.Default.TouchApp
                NodeType.VEHICLE_ACCEL -> Icons.Default.ArrowUpward
                NodeType.VEHICLE_REVERSE -> Icons.Default.ArrowDownward
                NodeType.VEHICLE_STEER_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
                NodeType.VEHICLE_STEER_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
                NodeType.VEHICLE_BRAKE -> Icons.Default.PanTool
                NodeType.VEHICLE_BOOST -> Icons.Default.Speed
                NodeType.VEHICLE_EXIT -> Icons.AutoMirrored.Filled.ExitToApp
                NodeType.VEHICLE_HORN -> Icons.Default.VolumeUp
                NodeType.VEHICLE_CHANGE_SEAT -> Icons.Default.AirlineSeatReclineExtra
                NodeType.VEHICLE_LEAN_DOWN -> Icons.Default.ArrowDownward
                NodeType.VEHICLE_LEAN_UP -> Icons.Default.ArrowUpward
                NodeType.ENTER_VEHICLE -> Icons.Default.DirectionsCar
                else -> null
            }

            if (icon != null && node.radiusDp >= 30f) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isKeyPressed) Color.White else nodeColor,
                    modifier = Modifier.size((node.radiusDp * 0.45f).coerceIn(14f, 22f).dp)
                )
            }

            Text(
                text = node.label,
                color = if (isKeyPressed) Color.White else TextPrimary,
                fontSize = (node.radiusDp * 0.28f).coerceIn(8.5f, 13f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Text(
                text = "[${node.boundInput}]",
                color = if (isKeyPressed) NeonAmber else nodeColor,
                fontSize = (node.radiusDp * 0.24f).coerceIn(7.5f, 11f).sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun JoystickNodeContent(
    node: MappingNode,
    nodeColor: Color,
    isEditMode: Boolean,
    isSelected: Boolean,
    isKeyPressed: Boolean,
    analogState: AnalogStickState
) {
    val isLeft = node.nodeType == NodeType.JOYSTICK_LEFT
    val stickX = if (isLeft) analogState.lx else analogState.rx
    val stickY = if (isLeft) analogState.ly else analogState.ry

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Color(0x1F0A101D).copy(alpha = node.opacity * 0.3f))
            .border(
                width = if (isSelected) 2.5.dp else 1.5.dp,
                color = if (isSelected) NeonAmber else nodeColor.copy(alpha = node.opacity),
                shape = CircleShape
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f

            drawLine(nodeColor.copy(alpha = 0.3f), Offset(cx - r, cy), Offset(cx + r, cy), 1f)
            drawLine(nodeColor.copy(alpha = 0.3f), Offset(cx, cy - r), Offset(cx, cy + r), 1f)

            drawCircle(
                color = Color(0x33FF5252),
                radius = r * node.stickDeadzone,
                center = Offset(cx, cy),
                style = Stroke(1f)
            )

            val knobRadius = r * 0.36f
            val maxTravel = r - knobRadius - 2f
            val knobX = cx + (stickX.coerceIn(-1f, 1f) * maxTravel)
            val knobY = cy + (stickY.coerceIn(-1f, 1f) * maxTravel)

            drawCircle(
                color = if (stickX != 0f || stickY != 0f) NeonCyan.copy(alpha = 0.7f) else Color(0x4424334F),
                radius = knobRadius,
                center = Offset(knobX, knobY)
            )
            drawCircle(
                color = if (stickX != 0f || stickY != 0f) Color.White else nodeColor,
                radius = knobRadius,
                center = Offset(knobX, knobY),
                style = Stroke(1.5f)
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = node.label,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "[${node.boundInput}]",
                    color = nodeColor,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
