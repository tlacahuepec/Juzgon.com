package com.juzgon.domain

private const val MIN_SCORE = 1
private const val MAX_SCORE = 10

data class Attribute(
    val id: String,
    val weight: Double = 1.0,
) {
    init {
        require(id.isNotBlank()) { "Attribute id cannot be blank" }
        require(weight > 0.0) { "Attribute weight must be greater than 0" }
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
) {
    init {
        require(id.isNotBlank()) { "Rated item id cannot be blank" }
        require(scores.map { it.attribute.id }.distinct().size == scores.size) {
            "Rated item scores must not contain duplicate attribute ids"
        }
    }
}
