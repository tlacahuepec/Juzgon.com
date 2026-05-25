package com.juzgon.data.backup

sealed class NormalizedAttributeId {
    data class Resolved(
        val id: String,
    ) : NormalizedAttributeId()

    data class Invalid(
        val reason: String,
    ) : NormalizedAttributeId()
}

object BackupAttributeIdNormalizer {
    fun normalize(
        rawId: String,
        categoryName: String?,
    ): NormalizedAttributeId =
        when {
            "/" in rawId -> NormalizedAttributeId.Resolved(rawId)
            categoryName.isNullOrEmpty() ->
                NormalizedAttributeId.Invalid(
                    "Cannot resolve short attribute id '$rawId' without category context",
                )
            else -> NormalizedAttributeId.Resolved("$categoryName/$rawId")
        }

    fun resolveOrThrow(
        rawId: String,
        categoryName: String?,
    ): String =
        when (val result = normalize(rawId, categoryName)) {
            is NormalizedAttributeId.Resolved -> result.id
            is NormalizedAttributeId.Invalid -> error(result.reason)
        }
}
