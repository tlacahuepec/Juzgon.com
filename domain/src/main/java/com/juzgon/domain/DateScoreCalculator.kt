package com.juzgon.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val YEARS_PER_STEP = 5
private const val MIN_SCORE = 1
private const val MAX_SCORE = 10

class DateScoreCalculator(
    private val clock: AppClock,
) {
    fun calculate(
        dateValue: String,
        direction: ScoringDirection,
    ): Int? {
        val date = runCatching { LocalDate.parse(dateValue) }.getOrNull() ?: return null
        val years =
            ChronoUnit.YEARS
                .between(date, clock.today())
                .toInt()
                .coerceAtLeast(0)
        return when (direction) {
            ScoringDirection.NEWER_IS_BETTER -> (MAX_SCORE - years / YEARS_PER_STEP).coerceIn(MIN_SCORE, MAX_SCORE)
            ScoringDirection.OLDER_IS_BETTER -> (MIN_SCORE + years / YEARS_PER_STEP).coerceIn(MIN_SCORE, MAX_SCORE)
        }
    }
}
