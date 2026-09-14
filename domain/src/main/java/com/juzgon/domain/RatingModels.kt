package com.juzgon.domain

private const val MIN_SCORE = 1
private const val MAX_SCORE = 10

data class Attribute(
    val id: String,
    val weight: Double = 1.0,
    val type: AttributeType = AttributeType.NUMBER,
    val isRequired: Boolean = true,
    val displayInDiamond: Boolean = type == AttributeType.NUMBER,
    val diamondOrder: Int? = null,
    val scoringDirection: ScoringDirection? = null,
    val suggestedValues: List<String> = emptyList(),
) {
    val displayName: String get() = id.substringAfter("/")

    val isRankable: Boolean
        get() = type == AttributeType.NUMBER || (type == AttributeType.DATE && scoringDirection != null)

    init {
        require(id.isNotBlank()) { "Attribute id cannot be blank" }
        require(weight > 0.0) { "Attribute weight must be greater than 0" }
        require(isRankable || !displayInDiamond) {
            "Only rankable attributes can be displayed in the diamond chart"
        }
        require(diamondOrder == null || diamondOrder > 0) {
            "Diamond order must be greater than 0"
        }
        require(scoringDirection == null || type == AttributeType.DATE) {
            "Scoring direction is only valid for DATE attributes"
        }
        require(suggestedValues.isEmpty() || type == AttributeType.DROPDOWN) {
            "Suggested values are only valid for DROPDOWN attributes"
        }
    }
}

data class RatingSystem(
    val attributes: List<Attribute>,
) {
    init {
        require(attributes.isNotEmpty()) { "Rating system must define at least one attribute" }
        require(attributes.map { it.id }.distinct().size == attributes.size) {
            "Rating system attributes must have unique ids"
        }
    }
}

data class ScoreEntry(
    val attribute: Attribute,
    val score: Int,
) {
    init {
        require(score in MIN_SCORE..MAX_SCORE) { "Score must be between 1 and 10" }
    }
}

data class RatedItem(
    val id: String,
    val scores: List<ScoreEntry>,
    val notes: String = "",
    val values: List<ItemAttributeValue> = emptyList(),
    val images: List<ItemImage> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    init {
        require(id.isNotBlank()) { "Rated item id cannot be blank" }
        require(scores.map { it.attribute.id }.distinct().size == scores.size) {
            "Rated item scores must not contain duplicate attribute ids"
        }
        require(values.map { it.attribute.id }.distinct().size == values.size) {
            "Rated item values must not contain duplicate attribute ids"
        }
    }
}

data class ItemAttributeValue(
    val attribute: Attribute,
    val value: String,
)

data class ItemImage(
    val id: String,
    val attribute: Attribute,
    val position: Int,
    val bytes: ByteArray,
    val mimeType: String? = null,
    val displayName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val createdAt: Long = 0L,
) {
    override fun equals(other: Any?): Boolean =
        other is ItemImage &&
            id == other.id &&
            attribute == other.attribute &&
            position == other.position &&
            bytes.contentEquals(other.bytes) &&
            mimeType == other.mimeType &&
            displayName == other.displayName &&
            width == other.width &&
            height == other.height &&
            createdAt == other.createdAt

    override fun hashCode(): Int =
        listOf(
            id,
            attribute,
            position,
            bytes.contentHashCode(),
            mimeType,
            displayName,
            width,
            height,
            createdAt,
        ).hashCode()
}

data class AttributeRankSnapshot(
    val itemId: String,
    val capturedAt: Long,
    val attributeId: String,
    val value: Int,
    val rank: Int,
)

data class RankedRatedItem(
    val item: RatedItem,
    val aggregateScore: Double,
)

data class Category(
    val name: String,
    val attributes: List<Attribute>,
    val itemCount: Int = 0,
    val description: String? = null,
    val type: CatalogType? = null,
)

fun buildAttributeRankSnapshots(
    itemId: String,
    capturedAt: Long,
    scores: List<ScoreEntry>,
): List<AttributeRankSnapshot> =
    scores
        .sortedWith(compareByDescending<ScoreEntry> { it.score }.thenBy { it.attribute.id })
        .mapIndexed { index, score ->
            AttributeRankSnapshot(
                itemId = itemId,
                capturedAt = capturedAt,
                attributeId = score.attribute.id,
                value = score.score,
                rank = index + 1,
            )
        }
