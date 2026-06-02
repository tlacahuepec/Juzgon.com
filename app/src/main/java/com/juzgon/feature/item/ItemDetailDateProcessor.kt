package com.juzgon.feature.item

import com.juzgon.domain.AttributeType
import com.juzgon.domain.BirthDateAgeCalculator
import com.juzgon.domain.DateScoreCalculator
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.ScoringDirection

class ItemDetailDateProcessor(
    private val birthDateAgeCalculator: BirthDateAgeCalculator,
    private val dateScoreCalculator: DateScoreCalculator,
) {
    fun computeAgeText(valueEntry: ItemAttributeValue): String? {
        if (valueEntry.attribute.type != AttributeType.DATE ||
            !isBirthDateName(valueEntry.attribute.displayName)
        ) {
            return null
        }
        return birthDateAgeCalculator.calculateAge(valueEntry.value)?.let { "Age: $it" }
    }

    fun computeDateScore(
        value: String,
        direction: ScoringDirection,
    ): Int? = dateScoreCalculator.calculate(value, direction)

    private fun isBirthDateName(displayName: String): Boolean = displayName.trim().lowercase() in BIRTH_DATE_NAMES

    companion object {
        private val BIRTH_DATE_NAMES = setOf("birthdate", "birth date")
    }
}
