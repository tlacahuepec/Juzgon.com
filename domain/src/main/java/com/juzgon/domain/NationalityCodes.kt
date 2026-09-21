package com.juzgon.domain

object NationalityCodes {
    fun parse(raw: String?): List<String> =
        raw
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    fun encode(codes: List<String>): String = codes.joinToString(",")

    fun primary(raw: String?): String? = parse(raw).firstOrNull()
}
