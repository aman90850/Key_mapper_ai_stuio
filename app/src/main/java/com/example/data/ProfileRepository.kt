package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ActionMode
import com.example.model.MappingNode
import com.example.model.NodeType
import com.example.model.Profile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProfileRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gamepad_mapper_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROFILES_JSON = "key_profiles_json"
        private const val KEY_ACTIVE_PROFILE_ID = "key_active_profile_id"
    }

    init {
        if (!prefs.contains(KEY_PROFILES_JSON)) {
            val initialProfiles = listOf(
                createShooterProProfile(),
                createFightingMasterProfile(),
                createKeyboardTacticalProfile()
            )
            saveAllProfiles(initialProfiles)
            prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, initialProfiles[0].id).apply()
        }
    }

    fun getAllProfiles(): List<Profile> {
        val jsonStr = prefs.getString(KEY_PROFILES_JSON, null) ?: return listOf(createShooterProProfile())
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Profile>()
            for (i in 0 until array.length()) {
                val p = parseProfileFromJson(array.getJSONObject(i))
                list.add(p)
            }
            if (list.isEmpty()) listOf(createShooterProProfile()) else list
        } catch (e: Exception) {
            listOf(createShooterProProfile())
        }
    }

    fun getActiveProfile(): Profile {
        val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
        val all = getAllProfiles()
        return all.find { it.id == activeId } ?: all.firstOrNull() ?: createShooterProProfile()
    }

    fun setActiveProfile(profileId: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply()
    }

    fun saveProfile(profile: Profile) {
        val all = getAllProfiles().toMutableList()
        val index = all.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            all[index] = profile.copy(updatedAt = System.currentTimeMillis())
        } else {
            all.add(profile)
        }
        saveAllProfiles(all)
    }

    fun createProfile(name: String, category: String = "Shooter", baseNodes: List<MappingNode> = emptyList()): Profile {
        val newProfile = Profile(
            id = UUID.randomUUID().toString(),
            name = name,
            category = category,
            nodes = if (baseNodes.isNotEmpty()) baseNodes else createShooterProProfile().nodes,
            updatedAt = System.currentTimeMillis()
        )
        saveProfile(newProfile)
        setActiveProfile(newProfile.id)
        return newProfile
    }

    fun duplicateProfile(profileId: String): Profile {
        val original = getAllProfiles().find { it.id == profileId } ?: getActiveProfile()
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            nodes = original.nodes.map { it.copy(id = UUID.randomUUID().toString()) },
            updatedAt = System.currentTimeMillis()
        )
        saveProfile(copy)
        setActiveProfile(copy.id)
        return copy
    }

    fun renameProfile(profileId: String, newName: String) {
        val all = getAllProfiles().toMutableList()
        val index = all.indexOfFirst { it.id == profileId }
        if (index >= 0) {
            all[index] = all[index].copy(name = newName, updatedAt = System.currentTimeMillis())
            saveAllProfiles(all)
        }
    }

    fun deleteProfile(profileId: String): Boolean {
        val all = getAllProfiles().toMutableList()
        if (all.size <= 1) return false // Prevent deleting last remaining profile
        all.removeAll { it.id == profileId }
        saveAllProfiles(all)
        if (prefs.getString(KEY_ACTIVE_PROFILE_ID, "") == profileId) {
            setActiveProfile(all.first().id)
        }
        return true
    }

    fun exportProfileToJson(profile: Profile): String {
        return serializeProfileToJson(profile).toString(2)
    }

    fun importProfileFromJson(jsonStr: String): Result<Profile> {
        return try {
            val jsonObject = JSONObject(jsonStr)
            val imported = parseProfileFromJson(jsonObject).copy(
                id = UUID.randomUUID().toString(),
                name = "${jsonObject.optString("name", "Imported Profile")} (Imported)",
                updatedAt = System.currentTimeMillis()
            )
            saveProfile(imported)
            setActiveProfile(imported.id)
            Result.success(imported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveAllProfiles(profiles: List<Profile>) {
        val array = JSONArray()
        profiles.forEach { array.put(serializeProfileToJson(it)) }
        prefs.edit().putString(KEY_PROFILES_JSON, array.toString()).apply()
    }

    private fun serializeProfileToJson(profile: Profile): JSONObject {
        val obj = JSONObject()
        obj.put("id", profile.id)
        obj.put("name", profile.name)
        obj.put("category", profile.category)
        obj.put("isDefault", profile.isDefault)
        obj.put("updatedAt", profile.updatedAt)

        val nodesArray = JSONArray()
        profile.nodes.forEach { node ->
            val nObj = JSONObject()
            nObj.put("id", node.id)
            nObj.put("nodeType", node.nodeType.name)
            nObj.put("label", node.label)
            nObj.put("boundInput", node.boundInput)
            nObj.put("keyCode", node.keyCode)
            nObj.put("xPercent", node.xPercent.toDouble())
            nObj.put("yPercent", node.yPercent.toDouble())
            nObj.put("radiusDp", node.radiusDp.toDouble())
            nObj.put("opacity", node.opacity.toDouble())
            nObj.put("colorHex", node.colorHex)
            nObj.put("actionMode", node.actionMode.name)
            nObj.put("stickDeadzone", node.stickDeadzone.toDouble())
            nObj.put("stickSensitivity", node.stickSensitivity.toDouble())
            nObj.put("macroSecondX", node.macroSecondX.toDouble())
            nObj.put("macroSecondY", node.macroSecondY.toDouble())
            nObj.put("macroDelayMs", node.macroDelayMs)
            nObj.put("turboRateHz", node.turboRateHz)
            nodesArray.put(nObj)
        }
        obj.put("nodes", nodesArray)
        return obj
    }

    private fun parseProfileFromJson(obj: JSONObject): Profile {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val name = obj.optString("name", "Custom Profile")
        val category = obj.optString("category", "General")
        val isDefault = obj.optBoolean("isDefault", false)
        val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

        val nodesList = mutableListOf<MappingNode>()
        val nodesArray = obj.optJSONArray("nodes")
        if (nodesArray != null) {
            for (i in 0 until nodesArray.length()) {
                val n = nodesArray.getJSONObject(i)
                val nodeType = try {
                    NodeType.valueOf(n.optString("nodeType", NodeType.STANDARD_TAP.name))
                } catch (e: Exception) {
                    NodeType.STANDARD_TAP
                }
                val actionMode = try {
                    ActionMode.valueOf(n.optString("actionMode", ActionMode.NORMAL_TAP.name))
                } catch (e: Exception) {
                    ActionMode.NORMAL_TAP
                }

                nodesList.add(
                    MappingNode(
                        id = n.optString("id", UUID.randomUUID().toString()),
                        nodeType = nodeType,
                        label = n.optString("label", "BTN"),
                        boundInput = n.optString("boundInput", "A"),
                        keyCode = n.optInt("keyCode", 0),
                        xPercent = n.optDouble("xPercent", 0.5).toFloat(),
                        yPercent = n.optDouble("yPercent", 0.5).toFloat(),
                        radiusDp = n.optDouble("radiusDp", 36.0).toFloat(),
                        opacity = n.optDouble("opacity", 0.85).toFloat(),
                        colorHex = n.optString("colorHex", "#00E5FF"),
                        actionMode = actionMode,
                        stickDeadzone = n.optDouble("stickDeadzone", 0.15).toFloat(),
                        stickSensitivity = n.optDouble("stickSensitivity", 1.0).toFloat(),
                        macroSecondX = n.optDouble("macroSecondX", 0.55).toFloat(),
                        macroSecondY = n.optDouble("macroSecondY", 0.55).toFloat(),
                        macroDelayMs = n.optLong("macroDelayMs", 120L),
                        turboRateHz = n.optInt("turboRateHz", 12)
                    )
                )
            }
        }

        return Profile(
            id = id,
            name = name,
            category = category,
            isDefault = isDefault,
            nodes = nodesList,
            updatedAt = updatedAt
        )
    }

    // Built-in presets
    private fun createShooterProProfile(): Profile {
        return Profile(
            id = "preset_shooter_pro",
            name = "BGMI / Shooter Pro",
            category = "Battle Royale / FPS",
            isDefault = true,
            nodes = listOf(
                MappingNode(
                    id = "node_move_stick",
                    nodeType = NodeType.JOYSTICK_LEFT,
                    label = "MOVE",
                    boundInput = "L-STICK",
                    xPercent = 0.18f,
                    yPercent = 0.68f,
                    radiusDp = 64f,
                    opacity = 0.80f,
                    colorHex = "#00E5FF"
                ),
                MappingNode(
                    id = "node_aim_stick",
                    nodeType = NodeType.JOYSTICK_RIGHT,
                    label = "LOOK",
                    boundInput = "R-STICK",
                    xPercent = 0.76f,
                    yPercent = 0.58f,
                    radiusDp = 70f,
                    opacity = 0.70f,
                    colorHex = "#448AFF"
                ),
                MappingNode(
                    id = "node_fire",
                    nodeType = NodeType.FIRE,
                    label = "FIRE",
                    boundInput = "RT",
                    xPercent = 0.88f,
                    yPercent = 0.35f,
                    radiusDp = 44f,
                    opacity = 0.90f,
                    colorHex = "#FF5252",
                    actionMode = ActionMode.NORMAL_TAP
                ),
                MappingNode(
                    id = "node_scope",
                    nodeType = NodeType.SCOPE,
                    label = "SCOPE",
                    boundInput = "LT",
                    xPercent = 0.82f,
                    yPercent = 0.18f,
                    radiusDp = 40f,
                    opacity = 0.85f,
                    colorHex = "#00E5FF",
                    actionMode = ActionMode.HOLD
                ),
                MappingNode(
                    id = "node_jump",
                    nodeType = NodeType.JUMP,
                    label = "JUMP",
                    boundInput = "A",
                    xPercent = 0.92f,
                    yPercent = 0.60f,
                    radiusDp = 38f,
                    opacity = 0.85f,
                    colorHex = "#FFB300"
                ),
                // SEPARATED CROUCH BUTTON
                MappingNode(
                    id = "node_crouch",
                    nodeType = NodeType.CROUCH,
                    label = "CROUCH",
                    boundInput = "B",
                    xPercent = 0.88f,
                    yPercent = 0.75f,
                    radiusDp = 38f,
                    opacity = 0.85f,
                    colorHex = "#26A69A"
                ),
                // SEPARATED PRONE BUTTON
                MappingNode(
                    id = "node_prone",
                    nodeType = NodeType.PRONE,
                    label = "PRONE",
                    boundInput = "R3",
                    xPercent = 0.78f,
                    yPercent = 0.86f,
                    radiusDp = 38f,
                    opacity = 0.85f,
                    colorHex = "#AB47BC"
                ),
                MappingNode(
                    id = "node_reload",
                    nodeType = NodeType.RELOAD,
                    label = "RELOAD",
                    boundInput = "X",
                    xPercent = 0.72f,
                    yPercent = 0.36f,
                    radiusDp = 36f,
                    opacity = 0.85f,
                    colorHex = "#448AFF"
                ),
                MappingNode(
                    id = "node_peek_l",
                    nodeType = NodeType.PEEK_LEFT,
                    label = "PEEK L",
                    boundInput = "LB",
                    xPercent = 0.15f,
                    yPercent = 0.35f,
                    radiusDp = 34f,
                    opacity = 0.80f,
                    colorHex = "#00E5FF",
                    actionMode = ActionMode.HOLD
                ),
                MappingNode(
                    id = "node_peek_r",
                    nodeType = NodeType.PEEK_RIGHT,
                    label = "PEEK R",
                    boundInput = "RB",
                    xPercent = 0.24f,
                    yPercent = 0.35f,
                    radiusDp = 34f,
                    opacity = 0.80f,
                    colorHex = "#00E5FF",
                    actionMode = ActionMode.HOLD
                ),
                MappingNode(
                    id = "node_sprint",
                    nodeType = NodeType.STANDARD_TAP,
                    label = "SPRINT",
                    boundInput = "L3",
                    xPercent = 0.18f,
                    yPercent = 0.46f,
                    radiusDp = 34f,
                    opacity = 0.80f,
                    colorHex = "#ECEFF1"
                ),
                MappingNode(
                    id = "node_gun1",
                    nodeType = NodeType.STANDARD_TAP,
                    label = "GUN 1",
                    boundInput = "D-PAD UP",
                    xPercent = 0.44f,
                    yPercent = 0.88f,
                    radiusDp = 36f,
                    opacity = 0.80f,
                    colorHex = "#00E5FF"
                ),
                MappingNode(
                    id = "node_gun2",
                    nodeType = NodeType.STANDARD_TAP,
                    label = "GUN 2",
                    boundInput = "D-PAD RIGHT",
                    xPercent = 0.56f,
                    yPercent = 0.88f,
                    radiusDp = 36f,
                    opacity = 0.80f,
                    colorHex = "#00E5FF"
                ),
                MappingNode(
                    id = "node_heal",
                    nodeType = NodeType.STANDARD_TAP,
                    label = "HEAL",
                    boundInput = "D-PAD DOWN",
                    xPercent = 0.34f,
                    yPercent = 0.88f,
                    radiusDp = 34f,
                    opacity = 0.80f,
                    colorHex = "#00E676"
                ),
                MappingNode(
                    id = "node_grenade",
                    nodeType = NodeType.STANDARD_TAP,
                    label = "GRENADE",
                    boundInput = "D-PAD LEFT",
                    xPercent = 0.66f,
                    yPercent = 0.88f,
                    radiusDp = 34f,
                    opacity = 0.80f,
                    colorHex = "#FF9100"
                ),
                MappingNode(
                    id = "node_backpack",
                    nodeType = NodeType.STANDARD_TAP,
                    label = "BAG",
                    boundInput = "SELECT",
                    xPercent = 0.08f,
                    yPercent = 0.88f,
                    radiusDp = 34f,
                    opacity = 0.80f,
                    colorHex = "#90CAF9"
                ),
                MappingNode(
                    id = "node_interact",
                    nodeType = NodeType.STANDARD_TAP,
                    label = "DOOR/REVIVE",
                    boundInput = "R3",
                    xPercent = 0.62f,
                    yPercent = 0.45f,
                    radiusDp = 36f,
                    opacity = 0.80f,
                    colorHex = "#FFD54F"
                )
            )
        )
    }

    fun resetActiveToPubgPreset(): Profile {
        val pubg = createShooterProProfile().copy(
            id = getActiveProfile().id,
            name = "BGMI / PUBG Pro HUD",
            updatedAt = System.currentTimeMillis()
        )
        saveProfile(pubg)
        return pubg
    }

    private fun createFightingMasterProfile(): Profile {
        return Profile(
            id = "preset_arcade_fighting",
            name = "Arcade / Fighting Master",
            category = "Arcade / Fighting",
            nodes = listOf(
                MappingNode(
                    id = "fight_stick",
                    nodeType = NodeType.JOYSTICK_LEFT,
                    label = "JOYSTICK",
                    boundInput = "L-STICK",
                    xPercent = 0.20f,
                    yPercent = 0.65f,
                    radiusDp = 68f,
                    opacity = 0.85f,
                    colorHex = "#00E5FF"
                ),
                MappingNode(id = "lp", nodeType = NodeType.STANDARD_TAP, label = "LP", boundInput = "X", xPercent = 0.70f, yPercent = 0.45f, colorHex = "#00E5FF"),
                MappingNode(id = "mp", nodeType = NodeType.STANDARD_TAP, label = "MP", boundInput = "Y", xPercent = 0.80f, yPercent = 0.38f, colorHex = "#FFB300"),
                MappingNode(id = "hp", nodeType = NodeType.STANDARD_TAP, label = "HP", boundInput = "RB", xPercent = 0.90f, yPercent = 0.35f, colorHex = "#FF5252"),
                MappingNode(id = "lk", nodeType = NodeType.STANDARD_TAP, label = "LK", boundInput = "A", xPercent = 0.70f, yPercent = 0.65f, colorHex = "#00E676"),
                MappingNode(id = "mk", nodeType = NodeType.STANDARD_TAP, label = "MK", boundInput = "B", xPercent = 0.80f, yPercent = 0.58f, colorHex = "#448AFF"),
                MappingNode(id = "hk", nodeType = NodeType.STANDARD_TAP, label = "HK", boundInput = "RT", xPercent = 0.90f, yPercent = 0.55f, colorHex = "#AB47BC"),
                MappingNode(
                    id = "super_combo",
                    nodeType = NodeType.COMBO_MACRO,
                    label = "EX COMBO",
                    boundInput = "LB",
                    xPercent = 0.80f,
                    yPercent = 0.78f,
                    macroSecondX = 0.90f,
                    macroSecondY = 0.55f,
                    macroDelayMs = 80L,
                    colorHex = "#FF3366",
                    actionMode = ActionMode.COMBO_MACRO
                )
            )
        )
    }

    private fun createKeyboardTacticalProfile(): Profile {
        return Profile(
            id = "preset_keyboard_wasd",
            name = "Keyboard WASD Tactical",
            category = "Keyboard / PC",
            nodes = listOf(
                MappingNode(
                    id = "kb_move",
                    nodeType = NodeType.JOYSTICK_LEFT,
                    label = "WASD",
                    boundInput = "WASD",
                    xPercent = 0.18f,
                    yPercent = 0.68f,
                    radiusDp = 64f,
                    opacity = 0.85f,
                    colorHex = "#00E5FF"
                ),
                MappingNode(id = "kb_fire", nodeType = NodeType.FIRE, label = "FIRE", boundInput = "SPACE", xPercent = 0.88f, yPercent = 0.35f, colorHex = "#FF5252"),
                MappingNode(id = "kb_scope", nodeType = NodeType.SCOPE, label = "AIM", boundInput = "E", xPercent = 0.82f, yPercent = 0.18f, colorHex = "#00E5FF", actionMode = ActionMode.HOLD),
                MappingNode(id = "kb_jump", nodeType = NodeType.JUMP, label = "JUMP", boundInput = "F", xPercent = 0.92f, yPercent = 0.60f, colorHex = "#FFB300"),
                // SEPARATED CROUCH & PRONE for Keyboard
                MappingNode(id = "kb_crouch", nodeType = NodeType.CROUCH, label = "CROUCH (C)", boundInput = "C", xPercent = 0.88f, yPercent = 0.75f, colorHex = "#26A69A"),
                MappingNode(id = "kb_prone", nodeType = NodeType.PRONE, label = "PRONE (Z)", boundInput = "Z", xPercent = 0.78f, yPercent = 0.86f, colorHex = "#AB47BC"),
                MappingNode(id = "kb_reload", nodeType = NodeType.RELOAD, label = "RELOAD (R)", boundInput = "R", xPercent = 0.72f, yPercent = 0.36f, colorHex = "#448AFF"),
                MappingNode(id = "kb_sprint", nodeType = NodeType.STANDARD_TAP, label = "SPRINT", boundInput = "SHIFT", xPercent = 0.18f, yPercent = 0.40f, colorHex = "#ECEFF1")
            )
        )
    }
}
