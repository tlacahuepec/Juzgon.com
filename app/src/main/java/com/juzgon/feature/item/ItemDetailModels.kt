package com.juzgon.feature.item

import java.util.Locale

private const val SCORE_MIN_DISPLAY = 0
private const val SCORE_MAX_DISPLAY = 10
private const val PERCENT_SCALE = 100
private const val SCORE_FORMAT = "%.1f"

data class ItemDetailAttributeScore(
    val label: String,
    val score: Int,
)

data class RankedAttributeCardUiModel(
    val rank: Int,
    val label: String,
    val valueText: String,
    val maxText: String,
    val progressPercent: Int,
    val progressFraction: Float,
) {
    val accessibleDescription: String
        get() = "Rank $rank, $label, $valueText out of $maxText, $progressPercent percent"
}

data class ItemDetailAttributeValue(
    val label: String,
    val value: String,
)

data class ItemDetailUiState(
    val itemId: String = "",
    val overallScoreText: String = "",
    val attributeScores: List<ItemDetailAttributeScore> = emptyList(),
    val rankedAttributes: List<RankedAttributeCardUiModel> = emptyList(),
    val attributeValues: List<ItemDetailAttributeValue> = emptyList(),
    val notes: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

internal fun rankedAttributeCards(attributeScores: List<ItemDetailAttributeScore>): List<RankedAttributeCardUiModel> =
    attributeScores
        .map { score ->
            score to score.score.coerceIn(SCORE_MIN_DISPLAY, SCORE_MAX_DISPLAY)
        }.sortedWith(
            compareByDescending<Pair<ItemDetailAttributeScore, Int>> { it.second }
                .thenBy { it.first.label.lowercase(Locale.ROOT) }
                .thenBy { it.first.label },
        ).mapIndexed { index, (score, displayScore) ->
            val progressPercent = displayScore * PERCENT_SCALE / SCORE_MAX_DISPLAY
            RankedAttributeCardUiModel(
                rank = index + 1,
                label = score.label,
                valueText = displayScore.toString(),
                maxText = SCORE_MAX_DISPLAY.toString(),
                progressPercent = progressPercent,
                progressFraction = progressPercent / PERCENT_SCALE.toFloat(),
            )
        }

internal fun computeWeightedAverageText(attributeScores: List<Pair<Double, Int>>): String {
    if (attributeScores.isEmpty()) return "—"
    val weightedSum = attributeScores.sumOf { (weight, score) -> weight * score }
    val totalWeight = attributeScores.sumOf { (weight, _) -> weight }
    return if (totalWeight == 0.0) "—" else String.format(Locale.US, SCORE_FORMAT, weightedSum / totalWeight)
}
