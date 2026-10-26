package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.ProfileRepository
import com.example.input.InputEventBus
import com.example.model.HudBackgroundType
import com.example.model.MappingNode
import com.example.model.NodeType
import com.example.model.PlayerSituation
import com.example.model.Profile
import com.example.ui.AddNodeMenu
import com.example.ui.HudWorkspace
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.util.UUID

class FloatingOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    private var windowManager: WindowManager? = null
    private var canvasComposeView: ComposeView? = null
    private var controlBarComposeView: ComposeView? = null
    private lateinit var profileRepository: ProfileRepository

    private var canvasParams: WindowManager.LayoutParams? = null
    private var controlBarParams: WindowManager.LayoutParams? = null

    // Shared State between canvas and control bar (Default 35% opacity for PUBG combat visibility)
    private var isEditModeState = mutableStateOf(true)
    private var opacityState = mutableFloatStateOf(0.35f)
    private var isCanvasVisibleState = mutableStateOf(true)
    private var activeProfileState = mutableStateOf<Profile?>(null)
    private var showAddNodeDialogState = mutableStateOf(false)
    private var isQuickMapModeState = mutableStateOf(false)
    private var mappingTargetNodeState = mutableStateOf<MappingNode?>(null)
    private var mapSuccessToastState = mutableStateOf<String?>(null)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        const val CHANNEL_ID = "gamepad_overlay_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        var isOverlayRunning = false

        fun start(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                context.stopService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        profileRepository = ProfileRepository(this)
        activeProfileState.value = profileRepository.getActiveProfile()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            cleanUpAndStop()
            return
        }

        startForegroundNotification()
        createSeparateViews()
        isOverlayRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            cleanUpAndStop()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gamepad Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating gamepad touch mapping over PUBG / BGMI"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gamepad Overlay Active")
            .setContentText("Buttons floating transparently over PUBG")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .addAction(0, "STOP / CLOSE OVERLAY", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createSeparateViews() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. FULLSCREEN CANVAS WINDOW: holds the transparent draggable buttons
        canvasParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        canvasComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@FloatingOverlayService))
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)

            setContent {
                val isVisible by isCanvasVisibleState
                val isEditMode by isEditModeState
                val opacity by opacityState
                val profile = activeProfileState.value ?: profileRepository.getActiveProfile()
                val currentSituation by profileRepository.currentSituation.collectAsState()
                val pressedKeys by InputEventBus.pressedKeys.collectAsState()
                val analogState by InputEventBus.analogState.collectAsState()
                val showAddDialog by showAddNodeDialogState

                // Filter nodes according to the player's situation (On-Foot vs Car vs Bike)
                val filteredNodes = profile.nodes.filter {
                    it.situation == "ALL" ||
                            it.situation.equals(currentSituation.id, ignoreCase = true) ||
                            it.situation.equals(currentSituation.name, ignoreCase = true)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    if (isVisible) {
                        HudWorkspace(
                            nodes = filteredNodes.map { it.copy(opacity = opacity) },
                            isEditMode = isEditMode,
                            selectedNode = mappingTargetNodeState.value,
                            pressedKeys = pressedKeys,
                            analogState = analogState,
                            hudBackground = HudBackgroundType.TRANSPARENT_OVERLAY,
                            onSelectNode = { clickedNode ->
                                if (isEditMode && clickedNode != null) {
                                    mappingTargetNodeState.value = clickedNode
                                    InputEventBus.activeKeyCaptureListener = { keyName, keyCode, _ ->
                                        val current = profileRepository.getActiveProfile()
                                        val updated = current.copy(
                                            nodes = current.nodes.map {
                                                if (it.id == clickedNode.id) it.copy(boundInput = keyName.uppercase(), keyCode = keyCode) else it
                                            }
                                        )
                                        profileRepository.saveProfile(updated)
                                        activeProfileState.value = updated
                                        mapSuccessToastState.value = "✅ MAPPED: [${clickedNode.label}] ➔ [$keyName]"
                                        mappingTargetNodeState.value = null
                                    }
                                }
                            },
                            onUpdateNodePosition = { nodeId, x, y ->
                                val current = profileRepository.getActiveProfile()
                                val updated = current.copy(
                                    nodes = current.nodes.map {
                                        if (it.id == nodeId) it.copy(xPercent = x, yPercent = y) else it
                                    }
                                )
                                profileRepository.saveProfile(updated)
                                activeProfileState.value = updated
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Mapping Prompt Banner when user clicks a virtual button
                    val target = mappingTargetNodeState.value
                    if (target != null && isEditMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFA090E17))
                                .border(2.dp, NeonAmber, RoundedCornerShape(16.dp))
                                .clickable {
                                    InputEventBus.activeKeyCaptureListener = null
                                    mappingTargetNodeState.value = null
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "MAPPING [${target.label.uppercase()}]",
                                        color = NeonAmber,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "Gamepad ka koi bhi button dabayein... (Tap to Cancel)",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Success Toast Banner
                    val toast = mapSuccessToastState.value
                    if (toast != null) {
                        LaunchedEffect(toast) {
                            kotlinx.coroutines.delay(2200L)
                            mapSuccessToastState.value = null
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xF000E676))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(toast, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    if (showAddDialog) {
                        AddNodeMenu(
                            onSpawn = { type ->
                                val current = profileRepository.getActiveProfile()
                                val defaultColor = when (type) {
                                    NodeType.FIRE, NodeType.RAPID_FIRE -> "#FF5252"
                                    NodeType.SCOPE -> "#00E5FF"
                                    NodeType.JUMP -> "#FFB300"
                                    NodeType.CROUCH -> "#26A69A"
                                    NodeType.PRONE -> "#AB47BC"
                                    NodeType.JOYSTICK_LEFT -> "#00E5FF"
                                    NodeType.JOYSTICK_RIGHT -> "#448AFF"
                                    else -> "#00E5FF"
                                }
                                val newNode = MappingNode(
                                    id = UUID.randomUUID().toString(),
                                    nodeType = type,
                                    label = type.defaultLabel,
                                    boundInput = type.defaultKey,
                                    xPercent = 0.5f,
                                    yPercent = 0.5f,
                                    radiusDp = if (type == NodeType.JOYSTICK_LEFT || type == NodeType.JOYSTICK_RIGHT) 60f else 38f,
                                    colorHex = defaultColor,
                                    situation = currentSituation.id
                                )
                                val updated = current.copy(nodes = current.nodes + newNode)
                                profileRepository.saveProfile(updated)
                                activeProfileState.value = updated
                                showAddNodeDialogState.value = false
                            },
                            onDismiss = { showAddNodeDialogState.value = false }
                        )
                    }
                }
            }
        }

        // 2. COMPACT TOP CONTROL BAR WINDOW: Always touchable so CLOSE and MODE switch always work!
        controlBarParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 12
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        controlBarComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@FloatingOverlayService))
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)

            setContent {
                val isEditMode by isEditModeState
                val opacity by opacityState
                val isCanvasVisible by isCanvasVisibleState

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xF00A0E17))
                        .border(1.5.dp, if (isEditMode) NeonAmber else NeonGreen, RoundedCornerShape(22.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = if (isEditMode) NeonAmber else NeonGreen,
                        modifier = Modifier.size(18.dp)
                    )

                    // 1. MODE TOGGLE: Move vs Locked
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isEditMode) Color(0x55FFB300) else Color(0x5500E676))
                            .clickable {
                                val nextEditMode = !isEditModeState.value
                                isEditModeState.value = nextEditMode
                                updateCanvasTouchFlags(!nextEditMode)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isEditMode) NeonAmber else NeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditMode) "MOVE BUTTONS" else "LOCKED (PLAY)",
                            color = if (isEditMode) NeonAmber else NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // 1c. SITUATIONAL CONTROLS SWITCHER (Foot vs Car vs Bike)
                    val currentSituation by profileRepository.currentSituation.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (currentSituation) {
                                    PlayerSituation.FOOT -> Color(0xFF004D40)
                                    PlayerSituation.VEHICLE_CAR -> Color(0xFFD84315)
                                    PlayerSituation.VEHICLE_BIKE -> Color(0xFF6A1B9A)
                                }
                            )
                            .border(
                                1.5.dp,
                                when (currentSituation) {
                                    PlayerSituation.FOOT -> NeonGreen
                                    PlayerSituation.VEHICLE_CAR -> NeonAmber
                                    PlayerSituation.VEHICLE_BIKE -> Color(0xFFE040FB)
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                val next = profileRepository.cycleSituation()
                                mapSuccessToastState.value = "⚡ SWITCHED: ${next.chipLabel}"
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentSituation.chipLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // 1b. TAP TO MAP HELPER (In edit mode)
                    if (isEditMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF332A00))
                                .border(1.dp, NeonAmber, RoundedCornerShape(12.dp))
                                .clickable {
                                    mapSuccessToastState.value = "⚡ Screen par kisi bhi button ko click karein aur Gamepad dabayein!"
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("TAP TO MAP", color = NeonAmber, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 2. "+ ADD" & "PUBG RESET" BUTTONS (In edit mode)
                    if (isEditMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF003844))
                                .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                                .clickable { showAddNodeDialogState.value = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+ ADD", color = NeonCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // 1-Tap PUBG Reset (Snaps all 16 buttons to exact official PUBG layout)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF232D3F))
                                .border(1.dp, NeonAmber, RoundedCornerShape(12.dp))
                                .clickable {
                                    val pubgProfile = profileRepository.resetActiveToPubgPreset()
                                    activeProfileState.value = pubgProfile
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("PUBG HUD", color = NeonAmber, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3. TACTICAL OPACITY SELECTOR (Optimized for PUBG combat clarity)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E2D44))
                            .clickable {
                                opacityState.floatValue = when (opacityState.floatValue) {
                                    0.35f -> 0.60f
                                    0.60f -> 0.15f
                                    0.15f -> 0.90f
                                    else -> 0.35f
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Opacity, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${(opacity * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 4. HIDE/SHOW BUTTONS
                    Icon(
                        imageVector = if (isCanvasVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Visibility",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable { isCanvasVisibleState.value = !isCanvasVisibleState.value }
                            .padding(2.dp)
                    )

                    // 5. PROMINENT BIG RED CLOSE BUTTON (Easy to tap, never misses!)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFB71C1C))
                            .clickable { cleanUpAndStop() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXIT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        try {
            windowManager?.addView(canvasComposeView, canvasParams)
            windowManager?.addView(controlBarComposeView, controlBarParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCanvasTouchFlags(touchThroughToPubg: Boolean) {
        val params = canvasParams ?: return
        val view = canvasComposeView ?: return
        if (touchThroughToPubg) {
            // In locked play mode, canvas is NOT touchable so touches and system gestures pass straight through!
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            // In edit mode, canvas IS touchable so user can drag and place buttons!
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun cleanUpAndStop() {
        isOverlayRunning = false
        try {
            canvasComposeView?.let {
                try {
                    windowManager?.removeViewImmediate(it)
                } catch (e: Exception) {
                    try { windowManager?.removeView(it) } catch (e2: Exception) {}
                }
            }
        } catch (e: Exception) {}
        canvasComposeView = null

        try {
            controlBarComposeView?.let {
                try {
                    windowManager?.removeViewImmediate(it)
                } catch (e: Exception) {
                    try { windowManager?.removeView(it) } catch (e2: Exception) {}
                }
            }
        } catch (e: Exception) {}
        controlBarComposeView = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUpAndStop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
