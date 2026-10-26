package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.material.icons.filled.PowerSettingsNew
import com.example.service.FloatingOverlayService
import com.example.model.HudBackgroundType
import com.example.model.Profile
import com.example.ui.theme.GamingBorder
import com.example.ui.theme.GamingSurface
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary

@Composable
fun TopControlBar(
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onOpenAddNode: () -> Unit,
    activeProfile: Profile,
    onOpenProfiles: () -> Unit,
    currentBg: HudBackgroundType,
    onSelectBg: (HudBackgroundType) -> Unit,
    onOpenBgmiOverlay: () -> Unit,
    isLandscape: Boolean,
    onToggleOrientation: () -> Unit,
    onResetToPubg: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var bgDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xF00A0E17))
            .border(1.dp, GamingBorder, RoundedCornerShape(0.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mode Switch (Edit vs Play)
        Button(
            onClick = onToggleEditMode,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEditMode) Color(0x33FFB300) else Color(0x3300E676)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isEditMode) NeonAmber else NeonGreen
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(38.dp).testTag("mode_toggle_button")
        ) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.SportsEsports,
                contentDescription = null,
                tint = if (isEditMode) NeonAmber else NeonGreen,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isEditMode) "EDIT BUTTONS" else "PLAY MODE",
                color = if (isEditMode) NeonAmber else NeonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // BGMI Floating Overlay Launcher / Stopper
        val isOverlayActive = FloatingOverlayService.isOverlayRunning
        Button(
            onClick = onOpenBgmiOverlay,
            colors = ButtonDefaults.buttonColors(containerColor = if (isOverlayActive) NeonRed else NeonGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(38.dp).testTag("open_bgmi_overlay_btn")
        ) {
            Icon(
                imageVector = if (isOverlayActive) Icons.Default.PowerSettingsNew else Icons.Default.Layers,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isOverlayActive) "STOP OVERLAY" else "BGMI OVERLAY",
                color = Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Screen Rotation Toggle for BGMI Landscape
        Button(
            onClick = onToggleOrientation,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D44)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(38.dp).testTag("rotate_screen_btn")
        ) {
            Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isLandscape) "SCREEN: HORIZONTAL (BGMI)" else "ROTATE: HORIZONTAL (BGMI)",
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Add Button (in Edit Mode)
        if (isEditMode) {
            Button(
                onClick = onOpenAddNode,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(38.dp).testTag("add_button_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ ADD BUTTON", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // Quick Tap-to-Map info button
            Button(
                onClick = { /* Tap-to-map is active when clicking nodes */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332A00)),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                modifier = Modifier.height(38.dp).testTag("tap_to_map_btn")
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("TAP TO MAP", color = NeonAmber, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }

            if (onResetToPubg != null) {
                Button(
                    onClick = onResetToPubg,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonAmber),
                    modifier = Modifier.height(38.dp).testTag("reset_pubg_layout_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PUBG DEFAULT", color = NeonAmber, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Profile Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141F32))
                .border(1.dp, Color(0xFF263959), RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenProfiles)
                .padding(horizontal = 10.dp)
                .testTag("profile_selector_btn")
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = activeProfile.name,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "Tap to switch",
                    color = NeonAmber,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Background switcher
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141F32))
                    .border(1.dp, GamingBorder, RoundedCornerShape(8.dp))
                    .clickable { bgDropdownExpanded = true }
                    .padding(horizontal = 10.dp)
                    .testTag("bg_switcher_btn")
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (currentBg) {
                        HudBackgroundType.TACTICAL_SHOOTER -> "BGMI Reference HUD"
                        HudBackgroundType.TRANSPARENT_OVERLAY -> "Transparent (PUBG)"
                        HudBackgroundType.CYBERPUNK_GRID -> "Grid"
                        HudBackgroundType.DARK_ARENA -> "Dark"
                    },
                    color = TextPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            DropdownMenu(
                expanded = bgDropdownExpanded,
                onDismissRequest = { bgDropdownExpanded = false },
                modifier = Modifier.background(GamingSurface)
            ) {
                HudBackgroundType.values().forEach { bg ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                bg.title,
                                color = if (currentBg == bg) NeonCyan else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            onSelectBg(bg)
                            bgDropdownExpanded = false
                        }
                    )
                }
            }
        }
    }
}
