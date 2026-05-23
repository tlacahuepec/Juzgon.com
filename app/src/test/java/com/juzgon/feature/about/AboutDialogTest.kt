package com.juzgon.feature.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.juzgon.domain.BuildMetadata
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AboutDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val testMetadata =
        BuildMetadata(
            versionName = "1.0",
            versionCode = 1,
            channel = "dev",
            gitSha = "abc1234",
            buildTimestamp = "1700000000000",
        )

    @Test
    fun aboutDialog_displaysVersionName() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(
                    metadata = testMetadata,
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("1.0", substring = true).assertIsDisplayed()
    }

    @Test
    fun aboutDialog_displaysChannel() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(
                    metadata = testMetadata,
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("dev", substring = true).assertIsDisplayed()
    }

    @Test
    fun aboutDialog_displaysGitSha() {
        composeRule.setContent {
            MaterialTheme {
                AboutDialog(
                    metadata = testMetadata,
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("abc1234", substring = true).assertIsDisplayed()
    }
}
