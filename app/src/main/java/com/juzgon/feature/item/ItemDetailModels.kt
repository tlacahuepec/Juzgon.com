package com.juzgon.feature.item

import java.util.Locale

private const val SCORE_FORMAT = "%.1f"

data class ItemDetailAttributeScore(
    val label: String,
    val score: Int,
)

data class ItemDetailUiState(
    val itemId: String = "",
    val overallScoreText: String = "",
    val attributeScores: List<ItemDetailAttributeScore> = emptyList(),
    val notes: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

internal fun computeWeightedAverageText(attributeScores: List<Pair<Double, Int>>): String {
    if (attributeScores.isEmpty()) return "—"
    val weightedSum = attributeScores.sumOf { (weight, score) -> weight * score }
    val totalWeight = attributeScores.sumOf { (weight, _) -> weight }
    return if (totalWeight == 0.0) "—" else String.format(Locale.US, SCORE_FORMAT, weightedSum / totalWeight)
}
