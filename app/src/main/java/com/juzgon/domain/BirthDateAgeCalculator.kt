package com.juzgon.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BirthDateAgeCalculator(
    private val clock: AppClock,
) {
    fun calculateAge(isoDate: String): Int? {
        val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return null
        return ChronoUnit.YEARS
            .between(date, clock.today())
            .toInt()
            .coerceAtLeast(0)
    }
}
