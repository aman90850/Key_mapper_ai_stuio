package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AddNodeMenu
import com.example.ui.AndroidBridgeDialog
import com.example.ui.HardwareMonitorBar
import com.example.ui.HudWorkspace
import com.example.ui.MainViewModel
import com.example.ui.NodeInspectorDialog
import com.example.ui.ProfileManagerDialog
import com.example.ui.TopControlBar
import com.example.ui.theme.GamingDarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = GamingDarkBackground
                ) { innerPadding ->
                    GamepadMapperScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.inputDetector.refreshDevices()
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return try {
            val handled = viewModel.inputDetector.handleKeyEvent(event)
            if (handled) return true

            val keyCode = event.keyCode
            val isGamingKey = KeyEvent.isGamepadButton(keyCode) ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    keyCode == KeyEvent.KEYCODE_SPACE ||
                    keyCode == KeyEvent.KEYCODE_TAB

            if (isGamingKey) {
                true
            } else {
                super.dispatchKeyEvent(event)
            }
        } catch (e: Exception) {
            super.dispatchKeyEvent(event)
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        return try {
            val handled = viewModel.inputDetector.handleMotionEvent(event)
            if (handled) true else super.dispatchGenericMotionEvent(event)
        } catch (e: Exception) {
            super.dispatchGenericMotionEvent(event)
        }
    }
}

@Composable
fun GamepadMapperScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val isAutoSpawn by viewModel.isAutoSpawnEnabled.collectAsState()
    val selectedNode by viewModel.selectedNode.collectAsState()
    val hudBackground by viewModel.hudBackground.collectAsState()
    val isListening by viewModel.isListeningForInput.collectAsState()

    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val activeDevice by viewModel.activeDevice.collectAsState()
    val lastInputEvent by viewModel.lastInputEvent.collectAsState()
    val analogState by viewModel.analogState.collectAsState()
    val pressedKeys by viewModel.pressedKeys.collectAsState()

    val showProfileDialog by viewModel.showProfileDialog.collectAsState()
    val showAddNodeMenu by viewModel.showAddNodeMenu.collectAsState()
    val showBridgeDialog by viewModel.showBridgeDialog.collectAsState()
    val statusToast by viewModel.statusToast.collectAsState()
    var showBgmiOverlayDialog by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GamingDarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Simplified Top Control Bar (Edit/Play Mode, BGMI Overlay, Rotate, Profile, Add Button, HUD Bg)
            TopControlBar(
                isEditMode = isEditMode,
                onToggleEditMode = { viewModel.toggleEditMode() },
                onOpenAddNode = { viewModel.openAddNodeMenu() },
                activeProfile = currentProfile,
                onOpenProfiles = { viewModel.openProfileDialog() },
                currentBg = hudBackground,
                onSelectBg = { viewModel.setHudBackground(it) },
                onOpenBgmiOverlay = { showBgmiOverlayDialog = true },
                isLandscape = isLandscape,
                onToggleOrientation = {
                    if (activity != null) {
                        val next = if (isLandscape) {
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                        activity.requestedOrientation = next
                    }
                },
                onResetToPubg = { viewModel.resetToPubgLayout() }
            )

            // 16:9 Interactive HUD Workspace Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HudWorkspace(
                    nodes = currentProfile.nodes,
                    isEditMode = isEditMode,
                    selectedNode = selectedNode,
                    pressedKeys = pressedKeys,
                    analogState = analogState,
                    hudBackground = hudBackground,
                    onSelectNode = { viewModel.selectNode(it) },
                    onUpdateNodePosition = { nodeId, x, y ->
                        viewModel.updateNodePosition(nodeId, x, y)
                    }
                )
            }

            // Live Gamepad Connection & Button Test Bar
            HardwareMonitorBar(
                devices = connectedDevices,
                activeDevice = activeDevice,
                lastEvent = lastInputEvent,
                analogState = analogState,
                onScan = { viewModel.scanGamepads() }
            )
        }

        // Animated Toast Banner
        AnimatedVisibility(
            visible = statusToast != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
        ) {
            statusToast?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2600)
                    viewModel.clearToast()
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xF2121A29), RoundedCornerShape(8.dp))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = msg,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Node Inspector Modal Dialog
        if (selectedNode != null && isEditMode) {
            NodeInspectorDialog(
                node = selectedNode!!,
                isListening = isListening,
                onStartListening = { viewModel.startListeningForNode(selectedNode!!.id) },
                onCancelListening = { viewModel.cancelListening() },
                onUpdateNode = { viewModel.updateNode(it) },
                onDeleteNode = { viewModel.deleteNode(selectedNode!!.id) },
                onDismiss = { viewModel.selectNode(null) }
            )
        }

        // Add Node Spawner Dialog
        if (showAddNodeMenu && isEditMode) {
            AddNodeMenu(
                onSpawn = { nodeType ->
                    viewModel.addNode(type = nodeType)
                },
                onDismiss = { viewModel.closeAddNodeMenu() }
            )
        }

        // Profile Manager Dialog
        if (showProfileDialog) {
            ProfileManagerDialog(
                activeProfile = currentProfile,
                profiles = allProfiles,
                onSwitchProfile = { viewModel.switchProfile(it) },
                onCreateProfile = { name, cat -> viewModel.createNewProfile(name, cat) },
                onDuplicateProfile = { viewModel.duplicateCurrentProfile() },
                onRenameProfile = { viewModel.renameCurrentProfile(it) },
                onDeleteProfile = { viewModel.deleteProfile(it) },
                onExportProfile = { viewModel.exportCurrentProfile() },
                onImportProfile = { viewModel.importProfile(it) },
                onDismiss = { viewModel.closeProfileDialog() }
            )
        }

        // BGMI Floating Overlay Launcher & Instructions Dialog
        if (showBgmiOverlayDialog) {
            com.example.ui.BgmiOverlayDialog(
                onDismiss = { showBgmiOverlayDialog = false }
            )
        }

        // Android Overlay & Touch Bridge Dialog
        if (showBridgeDialog) {
            AndroidBridgeDialog(
                overlayBridge = viewModel.overlayBridge,
                onDismiss = { viewModel.closeBridgeDialog() }
            )
        }
    }
}
