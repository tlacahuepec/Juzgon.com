@file:Suppress("MagicNumber")

package com.juzgon.domain

import java.util.Locale

data class SkinTypeValue(
    val storedValue: String,
    val displayLabel: String,
    val colorHex: String,
    val sortOrder: Int,
    val aliases: Set<String> = emptySet(),
)

object SkinTypeValues {
    val typeI =
        SkinTypeValue(
            storedValue = "TYPE_I",
            displayLabel = "Type I, very light",
            colorHex = "#F6D7C3",
            sortOrder = 1,
            aliases = setOf("I", "Type I"),
        )
    val typeII =
        SkinTypeValue(
            storedValue = "TYPE_II",
            displayLabel = "Type II",
            colorHex = "#EFC3A3",
            sortOrder = 2,
            aliases = setOf("II", "Type II"),
        )
    val typeIII =
        SkinTypeValue(
            storedValue = "TYPE_III",
            displayLabel = "Type III",
            colorHex = "#DFA37B",
            sortOrder = 3,
            aliases = setOf("III", "Type III"),
        )
    val typeIV =
        SkinTypeValue(
            storedValue = "TYPE_IV",
            displayLabel = "Type IV",
            colorHex = "#B87955",
            sortOrder = 4,
            aliases = setOf("IV", "Type IV"),
        )
    val typeV =
        SkinTypeValue(
            storedValue = "TYPE_V",
            displayLabel = "Type V",
            colorHex = "#7E4F35",
            sortOrder = 5,
            aliases = setOf("V", "Type V"),
        )
    val typeVI =
        SkinTypeValue(
            storedValue = "TYPE_VI",
            displayLabel = "Type VI, very dark",
            colorHex = "#4A2A1A",
            sortOrder = 6,
            aliases = setOf("VI", "Type VI"),
        )

    val entries: List<SkinTypeValue> = listOf(typeI, typeII, typeIII, typeIV, typeV, typeVI)

    fun fromStoredValue(value: String): SkinTypeValue? {
        val normalized = value.normalizeSkinTypeKey()
        return entries.firstOrNull { entry ->
            entry.matchKeys.any { it == normalized }
        }
    }

    fun displayLabelOrUnknown(value: String): String = fromStoredValue(value)?.displayLabel ?: UNKNOWN_LABEL

    fun sortOrderOrNull(value: String): Int? = fromStoredValue(value)?.sortOrder

    fun matchesQuery(
        value: String,
        query: String,
    ): Boolean {
        val entry = fromStoredValue(value) ?: return value.contains(query, ignoreCase = true)
        return entry.displayLabel.contains(query, ignoreCase = true) ||
            entry.storedValue.contains(query, ignoreCase = true) ||
            entry.aliases.any { alias -> alias.contains(query, ignoreCase = true) }
    }

    private val SkinTypeValue.matchKeys: Set<String>
        get() = (setOf(storedValue, displayLabel) + aliases).map { it.normalizeSkinTypeKey() }.toSet()

    private fun String.normalizeSkinTypeKey(): String =
        trim()
            .lowercase(Locale.US)
            .replace(",", "")
            .replace("_", " ")
            .replace(Regex("\\s+"), " ")

    private const val UNKNOWN_LABEL = "Unknown skin type"
}
