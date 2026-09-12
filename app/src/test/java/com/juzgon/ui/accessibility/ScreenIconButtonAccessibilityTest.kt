package com.juzgon.ui.accessibility

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.juzgon.ui.theme.JuzgonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreenIconButtonAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationIconButtonsHaveNonEmptyContentDescriptions() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete item")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit item")
                }
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete item").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Edit item").assertIsDisplayed()
    }
}
