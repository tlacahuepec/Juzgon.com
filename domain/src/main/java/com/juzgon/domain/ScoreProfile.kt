package com.juzgon.domain

data class ScoreProfile(
    val id: String,
    val categoryName: String,
    val name: String,
    val includedAttributeIds: List<String>,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    init {
        require(id.isNotBlank()) { "Score profile id cannot be blank" }
        require(categoryName.isNotBlank()) { "Score profile category name cannot be blank" }
        require(name.isNotBlank()) { "Score profile name cannot be blank" }
        require(includedAttributeIds.isNotEmpty()) { "Score profile must include at least one attribute" }
    }
}
