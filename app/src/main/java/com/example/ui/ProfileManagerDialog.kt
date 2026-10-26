package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Profile
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
fun ProfileManagerDialog(
    activeProfile: Profile,
    profiles: List<Profile>,
    onSwitchProfile: (String) -> Unit,
    onCreateProfile: (name: String, category: String) -> Unit,
    onDuplicateProfile: () -> Unit,
    onRenameProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onExportProfile: () -> String,
    onImportProfile: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    var newProfileName by remember { mutableStateOf("") }
    var newProfileCategory by remember { mutableStateOf("Shooter") }
    var renameInput by remember(activeProfile.id) { mutableStateOf(activeProfile.name) }
    var importJsonText by remember { mutableStateOf("") }
    var exportedJsonText by remember { mutableStateOf("") }
    var exportCopyMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .testTag("profile_manager_dialog"),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "MAPPING PROFILES MANAGER",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${profiles.size} saved configurations",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar (New Profile, Duplicate, Import, Export)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).testTag("create_profile_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("NEW", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDuplicateProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D44)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1.1f).testTag("duplicate_profile_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DUPLICATE", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            exportedJsonText = onExportProfile()
                            showExportDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D44)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1.1f).testTag("export_profile_btn")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D44)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1.1f).testTag("import_profile_btn")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IMPORT", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profile List
                LazyColumn(
                    modifier = Modifier.height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profiles) { profile ->
                        val isActive = profile.id == activeProfile.id
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) Color(0x2200E5FF) else GamingSurfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isActive) NeonCyan else GamingBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwitchProfile(profile.id) }
                                .testTag("profile_item_${profile.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) NeonCyan else Color(0xFF1A263B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isActive) {
                                            Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.Black, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = profile.name,
                                                color = if (isActive) NeonCyan else TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0x3300E5FF))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text("ACTIVE", color = NeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${profile.nodes.size} mapped touch nodes • ${profile.category}",
                                            color = TextMuted,
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isActive) {
                                        IconButton(
                                            onClick = {
                                                renameInput = profile.name
                                                showRenameDialog = true
                                            },
                                            modifier = Modifier.size(28.dp).testTag("rename_profile_btn")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    if (profiles.size > 1) {
                                        IconButton(
                                            onClick = { onDeleteProfile(profile.id) },
                                            modifier = Modifier.size(28.dp).testTag("delete_profile_${profile.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sub-Dialog: Create Profile
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = GamingSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GamingBorder),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CREATE NEW PROFILE", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name (e.g. Apex Mobile)", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GamingBorder
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_profile_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newProfileCategory,
                        onValueChange = { newProfileCategory = it },
                        label = { Text("Game Category (Shooter, MOBA, Arcade)", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GamingBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showCreateDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("CANCEL", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newProfileName.isNotBlank()) {
                                    onCreateProfile(newProfileName.trim(), newProfileCategory.trim())
                                    newProfileName = ""
                                    showCreateDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.testTag("confirm_create_profile_btn")
                        ) {
                            Text("CREATE", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Sub-Dialog: Rename Profile
    if (showRenameDialog) {
        Dialog(onDismissRequest = { showRenameDialog = false }) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = GamingSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GamingBorder),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("RENAME PROFILE", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Profile Name", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GamingBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showRenameDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("CANCEL", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (renameInput.isNotBlank()) {
                                    onRenameProfile(renameInput.trim())
                                    showRenameDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Sub-Dialog: Export Profile (.json)
    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = GamingSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GamingBorder),
                modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EXPORT PROFILE (.JSON)", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF070B12))
                            .border(1.dp, GamingBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = exportedJsonText,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (exportCopyMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(exportCopyMessage, color = NeonGreen, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showExportDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("CLOSE", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Gamepad Mapping Profile", exportedJsonText)
                                clipboard.setPrimaryClip(clip)
                                exportCopyMessage = "Copied profile JSON to clipboard!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            modifier = Modifier.testTag("copy_json_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("COPY JSON", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Sub-Dialog: Import Profile (.json)
    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = GamingSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GamingBorder),
                modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("IMPORT PROFILE (.JSON)", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = { Text("Paste Profile JSON here", color = TextSecondary, fontSize = 11.sp) },
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GamingBorder
                        ),
                        modifier = Modifier.fillMaxWidth().height(160.dp).testTag("import_json_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showImportDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text("CANCEL", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (importJsonText.isNotBlank()) {
                                    val success = onImportProfile(importJsonText.trim())
                                    if (success) showImportDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier.testTag("confirm_import_btn")
                        ) {
                            Text("IMPORT", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
