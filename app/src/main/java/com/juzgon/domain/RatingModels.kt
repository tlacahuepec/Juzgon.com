package com.juzgon.domain

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
    }
}

data class ScoreEntry(
    val attribute: Attribute,
    val score: Int,
) {
    init {
        require(score in 1..10) { "Score must be between 1 and 10" }
    }
}

data class RatedItem(
    val id: String,
    val scores: List<ScoreEntry>,
) {
    init {
        require(id.isNotBlank()) { "Rated item id cannot be blank" }
    }
}
