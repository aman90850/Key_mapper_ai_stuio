package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProfileRepository
import com.example.input.InputDetector
import com.example.model.NodeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gamepad Mapper", appName)
    }

    @Test
    fun `profile repository initializes default profiles with separated crouch and prone`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ProfileRepository(context)

        val profiles = repository.getAllProfiles()
        assertTrue("Profiles should not be empty", profiles.isNotEmpty())

        val active = repository.getActiveProfile()
        assertNotNull("Active profile should exist", active)

        val hasCrouch = active.nodes.any { it.nodeType == NodeType.CROUCH }
        val hasProne = active.nodes.any { it.nodeType == NodeType.PRONE }
        assertTrue("Active profile must contain dedicated Crouch button", hasCrouch)
        assertTrue("Active profile must contain dedicated Prone button", hasProne)
    }

    @Test
    fun `keycode translation produces expected key names`() {
        assertEquals("A", InputDetector.translateKeyCode(android.view.KeyEvent.KEYCODE_BUTTON_A, true))
        assertEquals("B", InputDetector.translateKeyCode(android.view.KeyEvent.KEYCODE_BUTTON_B, true))
        assertEquals("RT", InputDetector.translateKeyCode(android.view.KeyEvent.KEYCODE_BUTTON_R2, true))
        assertEquals("LT", InputDetector.translateKeyCode(android.view.KeyEvent.KEYCODE_BUTTON_L2, true))
        assertEquals("SPACE", InputDetector.translateKeyCode(android.view.KeyEvent.KEYCODE_SPACE, false))
        assertEquals("W", InputDetector.translateKeyCode(android.view.KeyEvent.KEYCODE_W, false))
        assertEquals("C", InputDetector.translateKeyCode(android.view.KeyEvent.KEYCODE_C, false))
        assertEquals("Z", InputDetector.translateKeyCode(android.view.KeyEvent.KEYCODE_Z, false))
    }
}
