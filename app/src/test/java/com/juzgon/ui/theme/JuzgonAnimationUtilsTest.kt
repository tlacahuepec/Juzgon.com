package com.juzgon.ui.theme

import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonAnimationUtilsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun animationsEnabledReturnsTrueWhenMotionNormal() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Settings.Global.putFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )

        var enabled = false
        composeRule.setContent {
            enabled = rememberAnimationsEnabled()
        }

        assertTrue(enabled)
    }

    @Test
    fun animationsEnabledReturnsFalseWhenReduceMotionEnabled() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Settings.Global.putFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            0f,
        )

        var enabled = true
        composeRule.setContent {
            enabled = rememberAnimationsEnabled()
        }

        assertFalse(enabled)
    }
}
