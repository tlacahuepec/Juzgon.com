@file:Suppress("FunctionName")

package com.juzgon.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.juzgon.ui.theme.JuzgonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JuzgonRadarChartTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersWithoutCrashWithValidPoints() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonRadarChart(
                    points =
                        listOf(
                            RadarChartPoint("Speed", 7f),
                            RadarChartPoint("Power", 8f),
                            RadarChartPoint("Skill", 6f),
                            RadarChartPoint("Defense", 5f),
                            RadarChartPoint("Stamina", 9f),
                        ),
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Radar chart", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun minimumThreePointsRendersCorrectly() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonRadarChart(
                    points =
                        listOf(
                            RadarChartPoint("A", 5f),
                            RadarChartPoint("B", 7f),
                            RadarChartPoint("C", 3f),
                        ),
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Radar chart", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun fewerThanThreePointsShowsFallbackState() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonRadarChart(
                    points =
                        listOf(
                            RadarChartPoint("A", 5f),
                            RadarChartPoint("B", 7f),
                        ),
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Radar chart requires at least 3 attributes")
            .assertIsDisplayed()
    }

    @Test
    fun semanticContentDescriptionSummarizesData() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonRadarChart(
                    points =
                        listOf(
                            RadarChartPoint("Speed", 9f),
                            RadarChartPoint("Power", 8f),
                            RadarChartPoint("Skill", 6f),
                            RadarChartPoint("Defense", 5f),
                            RadarChartPoint("Stamina", 7f),
                        ),
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Radar chart, 5 attributes, highest Speed 9 of 10")
            .assertIsDisplayed()
    }

    @Test
    fun compactModeDoesNotRenderLabels() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonRadarChart(
                    points =
                        listOf(
                            RadarChartPoint("Speed", 7f),
                            RadarChartPoint("Power", 8f),
                            RadarChartPoint("Skill", 6f),
                        ),
                    compact = true,
                )
            }
        }

        composeRule.onNodeWithText("Speed").assertDoesNotExist()
        composeRule.onNodeWithText("Power").assertDoesNotExist()
        composeRule.onNodeWithText("Skill").assertDoesNotExist()
    }

    @Test
    fun fullModeRendersLabelsForEachPoint() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonRadarChart(
                    points =
                        listOf(
                            RadarChartPoint("Speed", 7f),
                            RadarChartPoint("Power", 8f),
                            RadarChartPoint("Skill", 6f),
                        ),
                    compact = false,
                )
            }
        }

        composeRule.onNodeWithText("Speed").assertIsDisplayed()
        composeRule.onNodeWithText("Power").assertIsDisplayed()
        composeRule.onNodeWithText("Skill").assertIsDisplayed()
    }

    @Test
    fun handlesAllEqualValues() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonRadarChart(
                    points =
                        listOf(
                            RadarChartPoint("A", 5f),
                            RadarChartPoint("B", 5f),
                            RadarChartPoint("C", 5f),
                            RadarChartPoint("D", 5f),
                        ),
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Radar chart", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun handlesSingleMaxValue() {
        composeRule.setContent {
            JuzgonTheme(darkTheme = true, dynamicColor = false) {
                JuzgonRadarChart(
                    points =
                        listOf(
                            RadarChartPoint("A", 10f),
                            RadarChartPoint("B", 0f),
                            RadarChartPoint("C", 0f),
                        ),
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Radar chart", substring = true)
            .assertIsDisplayed()
    }
}
