package com.juzgon.testutil

import timber.log.Timber

class CapturingTimberTree : Timber.Tree() {
    data class LogEntry(
        val priority: Int,
        val tag: String?,
        val message: String,
    )

    val logs = mutableListOf<LogEntry>()

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        logs.add(LogEntry(priority, tag, message))
    }
}
