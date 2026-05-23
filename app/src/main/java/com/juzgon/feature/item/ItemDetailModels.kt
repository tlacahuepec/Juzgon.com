@file:Suppress("TooManyFunctions")

package com.juzgon.feature.item

import com.juzgon.domain.AttributeRankSnapshot
import com.juzgon.domain.AttributeType
import com.juzgon.domain.NationalityDataset
import java.text.SimpleDateFormat
import java.util.Locale

private const val SCORE_MIN_DISPLAY = 0
private const val SCORE_MAX_DISPLAY = 10
private const val PERCENT_SCALE = 100
private const val SCORE_FORMAT = "%.1f"
private const val FIRST_RANK = 1
private const val SECOND_RANK = 2
private const val THIRD_RANK = 3
private const val FOURTH_RANK = 4
private const val FIFTH_RANK = 5

data class ItemDetailAttributeScore(
    val label: String,
    val score: Int,
    val attributeId: String = label,
    val displayInDiamond: Boolean = true,
    val diamondOrder: Int? = null,
)

data class DiamondChartPoint(
    val label: String,
    val value: Int,
    val maxValue: Int = SCORE_MAX_DISPLAY,
) {
    val fraction: Float = value.coerceIn(SCORE_MIN_DISPLAY, SCORE_MAX_DISPLAY) / SCORE_MAX_DISPLAY.toFloat()
}

enum class AttributeRankSizeVariant {
    Rank1,
    Rank2,
    Rank3,
    Rank4,
    Rank5,
    Standard,
}

enum class AttributeMovementDirection {
    Improved,
    Declined,
    Unchanged,
}

data class AttributeMovement(
    val rank: AttributeMovementDirection,
    val value: AttributeMovementDirection,
)

data class RankedAttributeCardUiModel(
    val rank: Int,
    val label: String,
    val valueText: String,
    val maxText: String,
    val progressPercent: Int,
    val progressFraction: Float,
    val sizeVariant: AttributeRankSizeVariant = AttributeRankSizeVariant.Standard,
    val movement: AttributeMovement? = null,
) {
    val accessibleDescription: String
        get() =
            listOfNotNull(
                "Rank $rank, $label, $valueText out of $maxText, $progressPercent percent",
                movement?.accessibleDescription(),
            ).joinToString(", ")

    val testTag: String
        get() = "RankedAttributeCard:$sizeVariant:$rank"
}

data class ItemDetailAttributeValue(
    val label: String,
    val value: String,
    val type: AttributeType,
    val displayValue: String = formatAttributeValue(type, value),
    val imageReferences: List<ItemImageReference> = emptyList(),
)

data class ItemProfileBreakdown(
    val profileName: String,
    val profileScoreText: String,
    val profileRank: Int,
    val totalItems: Int,
    val includedAttributeIds: Set<String>,
)

data class ItemDetailUiState(
    val itemId: String = "",
    val primaryImage: ItemImageReference? = null,
    val overallScoreText: String = "",
    val attributeScores: List<ItemDetailAttributeScore> = emptyList(),
    val rankedAttributes: List<RankedAttributeCardUiModel> = emptyList(),
    val diamondChartPoints: List<DiamondChartPoint> = emptyList(),
    val attributeValues: List<ItemDetailAttributeValue> = emptyList(),
    val notes: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteCompleted: Boolean = false,
    val profileBreakdown: ItemProfileBreakdown? = null,
)

internal fun formatAttributeValue(
    type: AttributeType,
    value: String,
): String =
    when (type) {
        AttributeType.BOOLEAN -> if (value.equals("true", ignoreCase = true)) "Yes" else "No"
        AttributeType.DATE -> value.toDisplayDate()
        AttributeType.NATIONALITY ->
            NationalityDataset.findByCode(value)?.let { "${it.flagEmoji} ${it.nationality}" } ?: value
        else -> value
    }

internal fun rankedAttributeCards(
    attributeScores: List<ItemDetailAttributeScore>,
    previousSnapshots: List<AttributeRankSnapshot> = emptyList(),
): List<RankedAttributeCardUiModel> {
    val previousSnapshotsByAttributeId = previousSnapshots.associateBy { it.attributeId }
    return rankedAttributeCards(attributeScores, previousSnapshotsByAttributeId)
}

private fun rankedAttributeCards(
    attributeScores: List<ItemDetailAttributeScore>,
    previousSnapshotsByAttributeId: Map<String, AttributeRankSnapshot>,
): List<RankedAttributeCardUiModel> =
    attributeScores
        .map { score ->
            score to score.score.coerceIn(SCORE_MIN_DISPLAY, SCORE_MAX_DISPLAY)
        }.sortedWith(
            compareByDescending<Pair<ItemDetailAttributeScore, Int>> { it.second }
                .thenBy { it.first.label.lowercase(Locale.ROOT) }
                .thenBy { it.first.label },
        ).mapIndexed { index, (score, displayScore) ->
            val rank = index + 1
            val progressPercent = displayScore * PERCENT_SCALE / SCORE_MAX_DISPLAY
            RankedAttributeCardUiModel(
                rank = rank,
                label = score.label,
                valueText = displayScore.toString(),
                maxText = SCORE_MAX_DISPLAY.toString(),
                progressPercent = progressPercent,
                progressFraction = progressPercent / PERCENT_SCALE.toFloat(),
                sizeVariant = attributeRankSizeVariant(rank),
                movement =
                    attributeMovement(
                        currentRank = rank,
                        currentValue = score.score,
                        previousSnapshot = previousSnapshotsByAttributeId[score.attributeId],
                    ),
            )
        }

internal fun itemAttributeDiamondChartPoints(attributeScores: List<ItemDetailAttributeScore>): List<DiamondChartPoint> =
    attributeScores
        .filter { it.displayInDiamond }
        .sortedWith(
            compareBy<ItemDetailAttributeScore> { it.diamondOrder ?: Int.MAX_VALUE }
                .thenBy { it.label.lowercase(Locale.ROOT) }
                .thenBy { it.label },
        ).map { score ->
            DiamondChartPoint(
                label = score.label,
                value = score.score.coerceIn(SCORE_MIN_DISPLAY, SCORE_MAX_DISPLAY),
            )
        }

internal fun latestPreviousAttributeRankSnapshots(
    snapshots: List<AttributeRankSnapshot>,
    currentUpdatedAt: Long,
): List<AttributeRankSnapshot> {
    val latestPreviousCapturedAt =
        snapshots
            .asSequence()
            .map { it.capturedAt }
            .filter { it < currentUpdatedAt }
            .maxOrNull()
            ?: return emptyList()
    return snapshots.filter { it.capturedAt == latestPreviousCapturedAt }
}

internal fun attributeRankSizeVariant(rank: Int): AttributeRankSizeVariant =
    when (rank) {
        FIRST_RANK -> AttributeRankSizeVariant.Rank1
        SECOND_RANK -> AttributeRankSizeVariant.Rank2
        THIRD_RANK -> AttributeRankSizeVariant.Rank3
        FOURTH_RANK -> AttributeRankSizeVariant.Rank4
        FIFTH_RANK -> AttributeRankSizeVariant.Rank5
        else -> AttributeRankSizeVariant.Standard
    }

private fun attributeMovement(
    currentRank: Int,
    currentValue: Int,
    previousSnapshot: AttributeRankSnapshot?,
): AttributeMovement? =
    previousSnapshot?.let { snapshot ->
        AttributeMovement(
            rank = rankMovement(currentRank = currentRank, previousRank = snapshot.rank),
            value = valueMovement(currentValue = currentValue, previousValue = snapshot.value),
        )
    }

private fun rankMovement(
    currentRank: Int,
    previousRank: Int,
): AttributeMovementDirection =
    when {
        currentRank < previousRank -> AttributeMovementDirection.Improved
        currentRank > previousRank -> AttributeMovementDirection.Declined
        else -> AttributeMovementDirection.Unchanged
    }

private fun valueMovement(
    currentValue: Int,
    previousValue: Int,
): AttributeMovementDirection =
    when {
        currentValue > previousValue -> AttributeMovementDirection.Improved
        currentValue < previousValue -> AttributeMovementDirection.Declined
        else -> AttributeMovementDirection.Unchanged
    }

internal fun AttributeMovement.accessibleDescription(): String =
    listOf(
        "rank ${rank.accessibleLabel()}",
        "value ${value.accessibleLabel()}",
    ).joinToString(", ")

internal fun AttributeMovementDirection.accessibleLabel(): String =
    when (this) {
        AttributeMovementDirection.Improved -> "improved"
        AttributeMovementDirection.Declined -> "declined"
        AttributeMovementDirection.Unchanged -> "unchanged"
    }

private fun String.toDisplayDate(): String {
    val parsedDate =
        runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(this)
        }.getOrNull()
            ?: return this
    return SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsedDate)
}

internal fun computeWeightedAverageText(attributeScores: List<Pair<Double, Int>>): String {
    if (attributeScores.isEmpty()) return "—"
    val weightedSum = attributeScores.sumOf { (weight, score) -> weight * score }
    val totalWeight = attributeScores.sumOf { (weight, _) -> weight }
    return if (totalWeight == 0.0) "—" else String.format(Locale.US, SCORE_FORMAT, weightedSum / totalWeight)
}
