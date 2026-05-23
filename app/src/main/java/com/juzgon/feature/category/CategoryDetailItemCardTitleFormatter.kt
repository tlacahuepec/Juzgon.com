package com.juzgon.feature.category

internal object CategoryDetailItemCardTitleFormatter {
    data class ItemCardTitleParts(
        val primaryWord: String,
        val remainingTitle: String?,
    )

    fun split(title: String): ItemCardTitleParts {
        val words = title.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) {
            return ItemCardTitleParts(primaryWord = "", remainingTitle = null)
        }
        val primaryWord = words.first()
        val remainingTitle = words.drop(1).joinToString(" ").ifBlank { null }
        return ItemCardTitleParts(
            primaryWord = primaryWord,
            remainingTitle = remainingTitle,
        )
    }
}
