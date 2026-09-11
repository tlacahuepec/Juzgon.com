@file:Suppress("FunctionName")

package com.juzgon.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.juzgon.domain.BuildMetadata
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val testMetadata =
        BuildMetadata(
            versionName = "1.3.0",
            versionCode = 42,
            channel = "stable",
            gitSha = "abc1234",
            buildTimestamp = "2026-09-11",
        )

    @Test
    fun rendersSettingsScreenTitle() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    buildMetadata = testMetadata,
                    geminiKeyState = GeminiKeyState.NO_KEY,
                    maskedGeminiKey = null,
                    isExporting = false,
                    onExportBackup = {},
                    onNavigateToGeminiKey = {},
                )
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun rendersDatabaseBackupSectionWithExportButton() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    buildMetadata = testMetadata,
                    geminiKeyState = GeminiKeyState.NO_KEY,
                    maskedGeminiKey = null,
                    isExporting = false,
                    onExportBackup = {},
                    onNavigateToGeminiKey = {},
                )
            }
        }

        composeRule.onNodeWithText("Database & Backup").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Export backup")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun clickingExportButtonInvokesCallback() {
        var exportClicked = false

        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    buildMetadata = testMetadata,
                    geminiKeyState = GeminiKeyState.NO_KEY,
                    maskedGeminiKey = null,
                    isExporting = false,
                    onExportBackup = { exportClicked = true },
                    onNavigateToGeminiKey = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Export backup").performClick()
        assertTrue(exportClicked)
    }

    @Test
    fun rendersGeminiAISectionWithConfigureButton() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    buildMetadata = testMetadata,
                    geminiKeyState = GeminiKeyState.CONFIGURED,
                    maskedGeminiKey = "AIzaSy...7890",
                    isExporting = false,
                    onExportBackup = {},
                    onNavigateToGeminiKey = {},
                )
            }
        }

        composeRule.onNodeWithText("Gemini AI Configuration").assertIsDisplayed()
        composeRule.onNodeWithText("Key configured (AIzaSy...7890)").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Configure Gemini Key")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun clickingConfigureGeminiKeyInvokesNavigationCallback() {
        var configureClicked = false

        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    buildMetadata = testMetadata,
                    geminiKeyState = GeminiKeyState.NO_KEY,
                    maskedGeminiKey = null,
                    isExporting = false,
                    onExportBackup = {},
                    onNavigateToGeminiKey = { configureClicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Configure Gemini Key").performClick()
        assertTrue(configureClicked)
    }

    @Test
    fun rendersAboutSectionWithBuildMetadataAndContentDescription() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    buildMetadata = testMetadata,
                    geminiKeyState = GeminiKeyState.NO_KEY,
                    maskedGeminiKey = null,
                    isExporting = false,
                    onExportBackup = {},
                    onNavigateToGeminiKey = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("About").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Version 1.3.0 (42)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Channel: stable").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Build: abc1234").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rendersGeminiAISectionWhenNoKeyConfigured() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    buildMetadata = testMetadata,
                    geminiKeyState = GeminiKeyState.NO_KEY,
                    maskedGeminiKey = null,
                    isExporting = false,
                    onExportBackup = {},
                    onNavigateToGeminiKey = {},
                )
            }
        }

        composeRule
            .onNodeWithText(
                "No API key configured. AI autofill and scoring features are unavailable.",
            ).assertIsDisplayed()
    }

    @Test
    fun rendersExportingStateWhenExportInProgress() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    buildMetadata = testMetadata,
                    geminiKeyState = GeminiKeyState.NO_KEY,
                    maskedGeminiKey = null,
                    isExporting = true,
                    onExportBackup = {},
                    onNavigateToGeminiKey = {},
                )
            }
        }

        composeRule.onNodeWithText("Exporting...").assertIsDisplayed()
    }

    @Test
    fun rendersBackButtonAndInvokesCallback() {
        var backClicked = false

        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    buildMetadata = testMetadata,
                    geminiKeyState = GeminiKeyState.NO_KEY,
                    maskedGeminiKey = null,
                    isExporting = false,
                    onExportBackup = {},
                    onNavigateToGeminiKey = {},
                    onBackClick = { backClicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        assertTrue(backClicked)
    }
}
