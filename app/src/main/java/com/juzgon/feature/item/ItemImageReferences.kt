package com.juzgon.feature.item

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val IMAGE_REFERENCE_FORMAT_PREFIX = "imgref:v1|"
private const val IMAGE_REFERENCE_RECORD_SEPARATOR = "||"
private const val IMAGE_REFERENCE_FIELD_SEPARATOR = ";"
private const val IMAGE_REFERENCE_KEY_VALUE_SEPARATOR = "="
private const val IMAGE_REFERENCE_ID_KEY = "id"
private const val IMAGE_REFERENCE_SOURCE_URI_KEY = "src"
private const val IMAGE_REFERENCE_THUMBNAIL_URI_KEY = "thumb"
private const val IMAGE_REFERENCE_MIME_TYPE_KEY = "mime"
private const val IMAGE_REFERENCE_SIZE_BYTES_KEY = "size"
private const val IMAGE_REFERENCE_WIDTH_KEY = "w"
private const val IMAGE_REFERENCE_HEIGHT_KEY = "h"
private const val IMAGE_REFERENCE_CREATED_AT_KEY = "created"
private const val IMAGE_REFERENCE_DISPLAY_NAME_KEY = "name"
private val JSON_SOURCE_URI_REGEX = Regex("\"sourceUri\"\\s*:\\s*\"([^\"]+)\"")

data class ItemImageReference(
    val id: String,
    val sourceUri: String,
    val thumbnailUri: String = sourceUri,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val createdAt: Long = 0L,
    val displayName: String? = null,
)

@Suppress("LongParameterList")
internal fun buildImageReference(
    sourceUri: String,
    mimeType: String?,
    sizeBytes: Long?,
    width: Int?,
    height: Int?,
    displayName: String?,
    createdAt: Long,
): ItemImageReference =
    ItemImageReference(
        id = buildImageReferenceId(sourceUri = sourceUri, createdAt = createdAt),
        sourceUri = sourceUri,
        thumbnailUri = sourceUri,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        createdAt = createdAt,
        displayName = displayName,
    )

@Suppress("ReturnCount")
internal fun decodeItemImageReferences(valueText: String): List<ItemImageReference> {
    val trimmed = valueText.trim()
    if (trimmed.isBlank()) return emptyList()

    if (trimmed.startsWith(IMAGE_REFERENCE_FORMAT_PREFIX)) {
        val decoded = decodeV1References(trimmed.removePrefix(IMAGE_REFERENCE_FORMAT_PREFIX))
        if (decoded.isNotEmpty()) return decoded
    }

    val jsonDecoded = decodeJsonSourceUriReferences(trimmed)
    if (jsonDecoded.isNotEmpty()) return jsonDecoded

    return listOf(trimmed.toLegacyImageReference())
}

internal fun encodeItemImageReferences(references: List<ItemImageReference>): String {
    if (references.isEmpty()) return ""
    val payload =
        references.joinToString(IMAGE_REFERENCE_RECORD_SEPARATOR) { reference ->
            listOf(
                reference.toEncodedField(IMAGE_REFERENCE_ID_KEY, reference.id),
                reference.toEncodedField(IMAGE_REFERENCE_SOURCE_URI_KEY, reference.sourceUri),
                reference.toEncodedField(
                    IMAGE_REFERENCE_THUMBNAIL_URI_KEY,
                    reference.thumbnailUri.takeIf { thumbnailUri -> thumbnailUri != reference.sourceUri } ?: "",
                ),
                reference.toEncodedField(IMAGE_REFERENCE_MIME_TYPE_KEY, reference.mimeType ?: ""),
                reference.toEncodedField(IMAGE_REFERENCE_SIZE_BYTES_KEY, reference.sizeBytes?.toString().orEmpty()),
                reference.toEncodedField(IMAGE_REFERENCE_WIDTH_KEY, reference.width?.toString().orEmpty()),
                reference.toEncodedField(IMAGE_REFERENCE_HEIGHT_KEY, reference.height?.toString().orEmpty()),
                reference.toEncodedField(IMAGE_REFERENCE_CREATED_AT_KEY, reference.createdAt.toString()),
                reference.toEncodedField(IMAGE_REFERENCE_DISPLAY_NAME_KEY, reference.displayName ?: ""),
            ).joinToString(IMAGE_REFERENCE_FIELD_SEPARATOR)
        }
    return IMAGE_REFERENCE_FORMAT_PREFIX + payload
}

private fun decodeV1References(payload: String): List<ItemImageReference> =
    payload
        .split(IMAGE_REFERENCE_RECORD_SEPARATOR)
        .mapNotNull { record ->
            val fields = decodeFields(record)
            val sourceUri = fields[IMAGE_REFERENCE_SOURCE_URI_KEY].orEmpty().trim()
            if (sourceUri.isBlank()) return@mapNotNull null

            val createdAt = fields[IMAGE_REFERENCE_CREATED_AT_KEY].orEmpty().toLongOrNull() ?: 0L
            ItemImageReference(
                id =
                    fields[IMAGE_REFERENCE_ID_KEY]
                        ?.takeIf { id -> id.isNotBlank() }
                        ?: buildImageReferenceId(sourceUri = sourceUri, createdAt = createdAt),
                sourceUri = sourceUri,
                thumbnailUri = fields[IMAGE_REFERENCE_THUMBNAIL_URI_KEY].orEmpty().ifBlank { sourceUri },
                mimeType = fields[IMAGE_REFERENCE_MIME_TYPE_KEY]?.takeIf { mimeType -> mimeType.isNotBlank() },
                sizeBytes = fields[IMAGE_REFERENCE_SIZE_BYTES_KEY].orEmpty().toLongOrNull(),
                width = fields[IMAGE_REFERENCE_WIDTH_KEY].orEmpty().toIntOrNull(),
                height = fields[IMAGE_REFERENCE_HEIGHT_KEY].orEmpty().toIntOrNull(),
                createdAt = createdAt,
                displayName = fields[IMAGE_REFERENCE_DISPLAY_NAME_KEY]?.takeIf { name -> name.isNotBlank() },
            )
        }

private fun decodeFields(record: String): Map<String, String> =
    record
        .split(IMAGE_REFERENCE_FIELD_SEPARATOR)
        .mapNotNull { field ->
            val separatorIndex = field.indexOf(IMAGE_REFERENCE_KEY_VALUE_SEPARATOR)
            if (separatorIndex <= 0) return@mapNotNull null
            val key = field.substring(0, separatorIndex)
            val encodedValue = field.substring(separatorIndex + 1)
            key to encodedValue.urlDecode()
        }.toMap()

private fun decodeJsonSourceUriReferences(valueText: String): List<ItemImageReference> {
    val sourceUris =
        JSON_SOURCE_URI_REGEX
            .findAll(valueText)
            .map { match ->
                match.groupValues[1]
                    .replace("\\/", "/")
                    .replace("\\\\", "\\")
            }.toList()
    if (sourceUris.isEmpty()) return emptyList()
    return sourceUris.map { sourceUri -> sourceUri.toLegacyImageReference() }
}

private fun ItemImageReference.toEncodedField(
    key: String,
    value: String,
): String = "$key${IMAGE_REFERENCE_KEY_VALUE_SEPARATOR}${value.urlEncode()}"

private fun String.toLegacyImageReference(): ItemImageReference {
    val sourceUri = trim()
    return ItemImageReference(
        id = buildImageReferenceId(sourceUri = sourceUri, createdAt = 0L),
        sourceUri = sourceUri,
        thumbnailUri = sourceUri,
    )
}

private fun buildImageReferenceId(
    sourceUri: String,
    createdAt: Long,
): String = "$createdAt:${sourceUri.hashCode()}"

private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

private fun String.urlDecode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.toString())
