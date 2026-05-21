package com.juzgon.domain

import java.util.UUID

private const val MIN_SCORE = 1
private const val MAX_SCORE = 10

enum class AttributeType {
    NUMBER, DATE, BOOLEAN, DROPDOWN, URL, NOTES, IMAGE, RATING
}

data class Attribute(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AttributeType = AttributeType.RATING,
    val isRequired: Boolean = false,
    val weight: Double = 1.0,
    val position: Int = 0,
    val options: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Attribute id cannot be blank" }
        require(name.isNotBlank()) { "Attribute name cannot be blank" }
        if (type == AttributeType.RATING) {
            require(weight > 0.0) { "Attribute weight must be greater than 0" }
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

data class ItemValue(
    val attribute: Attribute,
    val valueString: String,
)

data class RatedItem(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val name: String,
    val scores: List<ScoreEntry> = emptyList(),
    val values: List<ItemValue> = emptyList(),
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    init {
        require(id.isNotBlank()) { "Rated item id cannot be blank" }
        require(categoryId.isNotBlank()) { "Category id cannot be blank" }
        require(name.isNotBlank()) { "Item name cannot be blank" }
        require(scores.map { it.attribute.id }.distinct().size == scores.size) {
            "Rated item scores must not contain duplicate attribute ids"
        }
    }
}

data class RankedRatedItem(
    val item: RatedItem,
    val aggregateScore: Double,
)

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val attributes: List<Attribute> = emptyList(),
    val itemCount: Int = 0,
) {
    init {
        require(id.isNotBlank()) { "Category id cannot be blank" }
        require(name.isNotBlank()) { "Category name cannot be blank" }
    }
}
