package com.juzgon.domain

import java.time.LocalDate

fun interface AppClock {
    fun today(): LocalDate
}
