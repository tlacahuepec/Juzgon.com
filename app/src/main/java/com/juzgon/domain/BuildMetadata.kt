package com.juzgon.domain

data class BuildMetadata(
    val versionName: String,
    val versionCode: Int,
    val channel: String,
    val gitSha: String,
    val buildTimestamp: String,
)

fun interface BuildMetadataProvider {
    fun get(): BuildMetadata
}
