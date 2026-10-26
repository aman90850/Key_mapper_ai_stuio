package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bridge.OverlayBridge
import com.example.data.ProfileRepository
import com.example.input.AnalogStickState
import com.example.input.InputDetector
import com.example.input.InputEvent
import com.example.input.InputEventBus
import com.example.model.ActionMode
import com.example.model.HardwareDevice
import com.example.model.HudBackgroundType
import com.example.model.MappingNode
import com.example.model.NodeType
import com.example.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)
    val overlayBridge = OverlayBridge(application)
    val inputDetector = InputDetector(application)

    private val _currentProfile = MutableStateFlow(repository.getActiveProfile())
    val currentProfile = _currentProfile.asStateFlow()

    private val _allProfiles = MutableStateFlow(repository.getAllProfiles())
    val allProfiles = _allProfiles.asStateFlow()

    private val _isEditMode = MutableStateFlow(true)
    val isEditMode = _isEditMode.asStateFlow()

    private val _isAutoSpawnEnabled = MutableStateFlow(false)
    val isAutoSpawnEnabled = _isAutoSpawnEnabled.asStateFlow()

    private val _selectedNode = MutableStateFlow<MappingNode?>(null)
    val selectedNode = _selectedNode.asStateFlow()

    private val _hudBackground = MutableStateFlow(HudBackgroundType.TACTICAL_SHOOTER)
    val hudBackground = _hudBackground.asStateFlow()

    private val _isListeningForInput = MutableStateFlow(false)
    val isListeningForInput = _isListeningForInput.asStateFlow()

    private val _listeningNodeId = MutableStateFlow<String?>(null)
    val listeningNodeId = _listeningNodeId.asStateFlow()

    // Dialog & sheet states
    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog = _showProfileDialog.asStateFlow()

    private val _showAddNodeMenu = MutableStateFlow(false)
    val showAddNodeMenu = _showAddNodeMenu.asStateFlow()

    private val _showBridgeDialog = MutableStateFlow(false)
    val showBridgeDialog = _showBridgeDialog.asStateFlow()

    private val _statusToast = MutableStateFlow<String?>(null)
    val statusToast = _statusToast.asStateFlow()

    // Real-time hardware streams
    val connectedDevices = inputDetector.connectedDevices
    val activeDevice = inputDetector.activeDevice
    val analogState = InputEventBus.analogState
    val lastInputEvent = InputEventBus.lastInputEvent
    val pressedKeys = InputEventBus.pressedKeys

    fun scanGamepads() {
        inputDetector.refreshDevices()
        val dev = inputDetector.activeDevice.value
        if (dev != null && dev.isConnected && dev.id != -1) {
            showToast("🎮 Detected: ${dev.name}")
        } else {
            showToast("🔍 Press any button on your Gamepad to register!")
        }
    }

    init {
        inputDetector.startListening()

        // Listen for hardware key events to handle auto-spawn, listening rebinding, and virtual touch
        InputEventBus.rawEvents.onEach { event ->
            if (event.isDown) {
                handleHardwareKeyDown(event)
            }
        }.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        inputDetector.stopListening()
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
        if (!_isEditMode.value) {
            _selectedNode.value = null
            _isListeningForInput.value = false
            showToast("Play & Lock Mode: Controls Locked. Tap buttons or use Gamepad/Keys!")
        } else {
            showToast("Edit Mode: Drag buttons, resize, and rebind keys freely.")
        }
    }

    fun setAutoSpawn(enabled: Boolean) {
        _isAutoSpawnEnabled.value = enabled
        if (enabled) {
            showToast("Auto-Spawn ON: Press any unmapped Gamepad button or Key to spawn it!")
        } else {
            showToast("Auto-Spawn OFF")
        }
    }

    fun setHudBackground(bg: HudBackgroundType) {
        _hudBackground.value = bg
    }

    fun selectNode(node: MappingNode?) {
        _selectedNode.value = node
    }

    fun updateNodePosition(nodeId: String, xPercent: Float, yPercent: Float) {
        val profile = _currentProfile.value
        val updatedNodes = profile.nodes.map {
            if (it.id == nodeId) it.copy(
                xPercent = xPercent.coerceIn(0.04f, 0.96f),
                yPercent = yPercent.coerceIn(0.04f, 0.96f)
            ) else it
        }
        val updatedProfile = profile.copy(nodes = updatedNodes)
        _currentProfile.value = updatedProfile
        repository.saveProfile(updatedProfile)

        if (_selectedNode.value?.id == nodeId) {
            _selectedNode.value = updatedNodes.find { it.id == nodeId }
        }
    }

    fun updateNode(updatedNode: MappingNode) {
        val profile = _currentProfile.value
        val updatedNodes = profile.nodes.map {
            if (it.id == updatedNode.id) updatedNode else it
        }
        val updatedProfile = profile.copy(nodes = updatedNodes)
        _currentProfile.value = updatedProfile
        repository.saveProfile(updatedProfile)
        _selectedNode.value = updatedNode
    }

    fun deleteNode(nodeId: String) {
        val profile = _currentProfile.value
        val updatedNodes = profile.nodes.filterNot { it.id == nodeId }
        val updatedProfile = profile.copy(nodes = updatedNodes)
        _currentProfile.value = updatedProfile
        repository.saveProfile(updatedProfile)
        if (_selectedNode.value?.id == nodeId) {
            _selectedNode.value = null
        }
        showToast("Button removed")
    }

    fun addNode(
        type: NodeType,
        customKey: String = "",
        customLabel: String = "",
        x: Float = 0.5f,
        y: Float = 0.5f
    ) {
        val profile = _currentProfile.value
        val label = customLabel.ifEmpty { type.defaultLabel }
        val key = customKey.ifEmpty { type.defaultKey }

        val defaultColor = when (type) {
            NodeType.FIRE, NodeType.RAPID_FIRE -> "#FF5252"
            NodeType.SCOPE -> "#00E5FF"
            NodeType.JUMP -> "#FFB300"
            NodeType.CROUCH -> "#26A69A" // Dedicated Crouch color
            NodeType.PRONE -> "#AB47BC"  // Dedicated Prone color
            NodeType.JOYSTICK_LEFT -> "#00E5FF"
            NodeType.JOYSTICK_RIGHT -> "#448AFF"
            NodeType.COMBO_MACRO -> "#FF3366"
            else -> "#00E5FF"
        }

        val defaultRadius = when (type) {
            NodeType.JOYSTICK_LEFT, NodeType.JOYSTICK_RIGHT -> 64f
            NodeType.FIRE -> 44f
            else -> 36f
        }

        val defaultActionMode = when (type) {
            NodeType.COMBO_MACRO -> ActionMode.COMBO_MACRO
            NodeType.RAPID_FIRE -> ActionMode.RAPID_TURBO
            NodeType.SCOPE, NodeType.PEEK_LEFT, NodeType.PEEK_RIGHT -> ActionMode.HOLD
            else -> ActionMode.NORMAL_TAP
        }

        val newNode = MappingNode(
            id = UUID.randomUUID().toString(),
            nodeType = type,
            label = label,
            boundInput = key.uppercase(),
            xPercent = x.coerceIn(0.08f, 0.92f),
            yPercent = y.coerceIn(0.08f, 0.92f),
            radiusDp = defaultRadius,
            opacity = 0.85f,
            colorHex = defaultColor,
            actionMode = defaultActionMode
        )

        val updatedProfile = profile.copy(nodes = profile.nodes + newNode)
        _currentProfile.value = updatedProfile
        repository.saveProfile(updatedProfile)
        _selectedNode.value = newNode
        showToast("Spawned $label mapped to [$key]")
    }

    fun startListeningForNode(nodeId: String) {
        _listeningNodeId.value = nodeId
        _isListeningForInput.value = true
        InputEventBus.activeKeyCaptureListener = { keyName, keyCode, _ ->
            rebindNode(nodeId, keyName, keyCode)
        }
    }

    fun cancelListening() {
        _isListeningForInput.value = false
        _listeningNodeId.value = null
        InputEventBus.activeKeyCaptureListener = null
    }

    private fun rebindNode(nodeId: String, keyName: String, keyCode: Int) {
        val profile = _currentProfile.value
        val updatedNodes = profile.nodes.map {
            if (it.id == nodeId) {
                it.copy(boundInput = keyName.uppercase(), keyCode = keyCode)
            } else it
        }
        val updatedProfile = profile.copy(nodes = updatedNodes)
        _currentProfile.value = updatedProfile
        repository.saveProfile(updatedProfile)
        _selectedNode.value = updatedNodes.find { it.id == nodeId }
        _isListeningForInput.value = false
        _listeningNodeId.value = null
        showToast("Rebound to [$keyName]")
    }

    private fun handleHardwareKeyDown(event: InputEvent) {
        // If listening for input
        val listeningId = _listeningNodeId.value
        if (_isListeningForInput.value && listeningId != null) {
            rebindNode(listeningId, event.keyName, event.keyCode)
            return
        }

        val profile = _currentProfile.value
        val matchingNodes = profile.nodes.filter { it.boundInput.equals(event.keyName, ignoreCase = true) }

        // Auto-Spawn mode: If unmapped key pressed in edit mode, auto spawn a new button!
        if (matchingNodes.isEmpty() && _isAutoSpawnEnabled.value && _isEditMode.value) {
            // Pick a smart spot (e.g. center with slight jitter)
            val jitterX = 0.45f + (Math.random().toFloat() * 0.1f)
            val jitterY = 0.45f + (Math.random().toFloat() * 0.1f)
            addNode(
                type = NodeType.STANDARD_TAP,
                customKey = event.keyName,
                customLabel = event.keyName,
                x = jitterX,
                y = jitterY
            )
            return
        }

        // Virtual Touch Injection Trigger for connected nodes
        matchingNodes.forEach { node ->
            when (node.actionMode) {
                ActionMode.COMBO_MACRO -> {
                    overlayBridge.injectComboMacro(
                        x1 = node.xPercent,
                        y1 = node.yPercent,
                        x2 = node.macroSecondX,
                        y2 = node.macroSecondY,
                        delayMs = node.macroDelayMs
                    )
                }
                ActionMode.NORMAL_TAP -> {
                    overlayBridge.injectTap(node.xPercent, node.yPercent, 60L)
                }
                ActionMode.HOLD -> {
                    overlayBridge.injectDown(node.xPercent, node.yPercent)
                }
                ActionMode.RAPID_TURBO -> {
                    overlayBridge.injectTap(node.xPercent, node.yPercent, 40L)
                }
            }
        }
    }

    // Profile Management
    fun openProfileDialog() { _showProfileDialog.value = true }
    fun closeProfileDialog() { _showProfileDialog.value = false }

    fun openAddNodeMenu() { _showAddNodeMenu.value = true }
    fun closeAddNodeMenu() { _showAddNodeMenu.value = false }

    fun openBridgeDialog() { _showBridgeDialog.value = true }
    fun closeBridgeDialog() { _showBridgeDialog.value = false }

    fun switchProfile(profileId: String) {
        repository.setActiveProfile(profileId)
        val profile = repository.getActiveProfile()
        _currentProfile.value = profile
        _allProfiles.value = repository.getAllProfiles()
        _selectedNode.value = null
        showToast("Switched to \"${profile.name}\"")
    }

    fun createNewProfile(name: String, category: String) {
        val created = repository.createProfile(name, category)
        _currentProfile.value = created
        _allProfiles.value = repository.getAllProfiles()
        showToast("Created profile \"${created.name}\"")
    }

    fun duplicateCurrentProfile() {
        val current = _currentProfile.value
        val copy = repository.duplicateProfile(current.id)
        _currentProfile.value = copy
        _allProfiles.value = repository.getAllProfiles()
        showToast("Duplicated as \"${copy.name}\"")
    }

    fun renameCurrentProfile(newName: String) {
        val current = _currentProfile.value
        repository.renameProfile(current.id, newName)
        _currentProfile.value = current.copy(name = newName)
        _allProfiles.value = repository.getAllProfiles()
        showToast("Renamed to \"$newName\"")
    }

    fun deleteProfile(profileId: String) {
        val deleted = repository.deleteProfile(profileId)
        if (deleted) {
            _currentProfile.value = repository.getActiveProfile()
            _allProfiles.value = repository.getAllProfiles()
            showToast("Profile deleted")
        } else {
            showToast("Cannot delete the only remaining profile")
        }
    }

    fun exportCurrentProfile(): String {
        return repository.exportProfileToJson(_currentProfile.value)
    }

    fun importProfile(jsonStr: String): Boolean {
        val result = repository.importProfileFromJson(jsonStr)
        return if (result.isSuccess) {
            val p = result.getOrThrow()
            _currentProfile.value = p
            _allProfiles.value = repository.getAllProfiles()
            showToast("Profile \"${p.name}\" imported successfully!")
            true
        } else {
            showToast("Failed to parse profile JSON: ${result.exceptionOrNull()?.message}")
            false
        }
    }

    fun resetToPubgLayout() {
        val pubg = repository.resetActiveToPubgPreset()
        _currentProfile.value = pubg
        _allProfiles.value = repository.getAllProfiles()
        showToast("Reset to official BGMI / PUBG Pro HUD Layout!")
    }

    fun clearToast() { _statusToast.value = null }
    private fun showToast(msg: String) {
        _statusToast.value = msg
    }
}
