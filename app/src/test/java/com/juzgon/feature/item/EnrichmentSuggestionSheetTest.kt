@file:Suppress("FunctionName")

package com.juzgon.feature.item

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.juzgon.domain.enrichment.EnrichmentConfidence
import com.juzgon.domain.enrichment.EnrichmentSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EnrichmentSuggestionSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun foundWithSources_showsSourceCount() {
        setFoundContent(
            sources =
                listOf(
                    EnrichmentSource(title = "Wikipedia", url = "https://en.wikipedia.org"),
                    EnrichmentSource(title = "IMDb", url = "https://imdb.com"),
                ),
        )

        composeRule.onNodeWithText("Sources: 2", substring = true).assertIsDisplayed()
    }

    @Test
    fun foundWithSources_expandShowsSourceTitles() {
        setFoundContent(
            sources =
                listOf(
                    EnrichmentSource(title = "Wikipedia", url = "https://en.wikipedia.org"),
                    EnrichmentSource(title = "IMDb", url = "https://imdb.com"),
                ),
        )

        composeRule.onNodeWithText("Sources: 2", substring = true).performClick()
        composeRule.onNodeWithText("Wikipedia").assertIsDisplayed()
        composeRule.onNodeWithText("IMDb").assertIsDisplayed()
    }

    @Test
    fun foundWithMissingTitle_showsFallbackSourceN() {
        setFoundContent(
            sources =
                listOf(
                    EnrichmentSource(title = null, url = "https://example.com"),
                ),
        )

        composeRule.onNodeWithText("Sources: 1", substring = true).performClick()
        composeRule.onNodeWithText("Source 1").assertIsDisplayed()
    }

    @Test
    fun foundWithEmptySources_showsNoSourcesMessage() {
        setFoundContent(sources = emptyList())

        composeRule.onNodeWithText("No source details available").assertIsDisplayed()
    }

    @Test
    fun foundWithSourceUrl_showsOpenLinkButton() {
        setFoundContent(
            sources =
                listOf(
                    EnrichmentSource(title = "Wikipedia", url = "https://en.wikipedia.org"),
                ),
        )

        composeRule.onNodeWithText("Sources: 1", substring = true).performClick()
        composeRule.onNodeWithText("Open link").assertIsDisplayed()
    }

    @Test
    fun foundWithNullUrl_hidesOpenLinkButton() {
        setFoundContent(
            sources =
                listOf(
                    EnrichmentSource(title = "Wikipedia", url = null),
                ),
        )

        composeRule.onNodeWithText("Sources: 1", substring = true).performClick()
        composeRule.onNodeWithText("Wikipedia").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Open link").fetchSemanticsNodes().size)
    }

    @Test
    fun foundWithSnippet_showsSnippetText() {
        setFoundContent(
            sources =
                listOf(
                    EnrichmentSource(
                        title = "Wikipedia",
                        url = null,
                        snippet = "Born June 24, 1987 in Rosario",
                    ),
                ),
        )

        composeRule.onNodeWithText("Sources: 1", substring = true).performClick()
        composeRule.onNodeWithText("Born June 24, 1987 in Rosario").assertIsDisplayed()
    }

    @Test
    fun notFound_showsRetryButton_whenCanRetry() {
        var retryClicked = false
        composeRule.setContent {
            MaterialTheme {
                EnrichmentSuggestionSheet(
                    state = EnrichmentSheetState.NotFound(reason = "Not found"),
                    canRetry = true,
                    onAccept = {},
                    onConflictResolved = {},
                    onDismiss = {},
                    onRetry = { retryClicked = true },
                    onNavigateToSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(true, retryClicked)
    }

    @Test
    fun notFound_hidesRetryButton_whenCannotRetry() {
        composeRule.setContent {
            MaterialTheme {
                EnrichmentSuggestionSheet(
                    state = EnrichmentSheetState.NotFound(reason = "Not found"),
                    canRetry = false,
                    onAccept = {},
                    onConflictResolved = {},
                    onDismiss = {},
                    onRetry = {},
                    onNavigateToSettings = {},
                )
            }
        }

        assertEquals(0, composeRule.onAllNodesWithText("Retry").fetchSemanticsNodes().size)
    }

    @Test
    fun error_showsRetryButton_whenCanRetry() {
        composeRule.setContent {
            MaterialTheme {
                EnrichmentSuggestionSheet(
                    state = EnrichmentSheetState.Error(failureCode = null, reason = null),
                    canRetry = true,
                    onAccept = {},
                    onConflictResolved = {},
                    onDismiss = {},
                    onRetry = {},
                    onNavigateToSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun conflict_showsRetryButton_whenCanRetry() {
        composeRule.setContent {
            MaterialTheme {
                EnrichmentSuggestionSheet(
                    state =
                        EnrichmentSheetState.Conflict(
                            attributeId = "birthDate",
                            attributeLabel = "Birth Date",
                            reason = null,
                            sources = emptyList(),
                            candidateValues = emptyList(),
                        ),
                    canRetry = true,
                    onAccept = {},
                    onConflictResolved = {},
                    onDismiss = {},
                    onRetry = {},
                    onNavigateToSettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    private fun setFoundContent(sources: List<EnrichmentSource>) {
        val state =
            EnrichmentSheetState.Found(
                attributeId = "birthDate",
                attributeLabel = "Birth Date",
                suggestedValue = "1987-06-24",
                displayValue = "June 24, 1987",
                confidence = EnrichmentConfidence.HIGH,
                sources = sources,
            )
        composeRule.setContent {
            MaterialTheme {
                EnrichmentSuggestionSheet(
                    state = state,
                    canRetry = false,
                    onAccept = {},
                    onConflictResolved = {},
                    onDismiss = {},
                    onRetry = {},
                    onNavigateToSettings = {},
                )
            }
        }
    }
}
