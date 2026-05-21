package com.juzgon.domain

import java.math.RoundingMode
import java.text.DecimalFormat

private const val HUNDRED_SCALE_FACTOR = 10

class ScorePresentationAdapter {
    fun format(score: Int): String = score.toString()

    fun toHundredScale(score: Int): Int = score * HUNDRED_SCALE_FACTOR

    fun formatHundredScale(score: Int): String = toHundredScale(score).toString()

    fun formatAverage(average: Double): String {
        val df = DecimalFormat("0.0")
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(average)
    }
}
