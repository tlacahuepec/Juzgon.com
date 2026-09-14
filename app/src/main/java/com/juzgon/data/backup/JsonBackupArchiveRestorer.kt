package com.juzgon.data.backup

import com.juzgon.data.local.entity.ItemImageEntity
import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupValidator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

internal const val ARCHIVE_FORMAT_VERSION = 1
internal const val MANIFEST_ENTRY = "manifest.json"
internal const val DATA_ENTRY = "data.json"
internal const val IMAGE_DIRECTORY = "images/"

class JsonBackupArchiveRestorer(
    private val validator: BackupValidator,
    private val restorer: JsonBackupRestorer,
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit,
    private val runPostImportMaintenance: suspend () -> Unit,
) {
    suspend fun restoreArchive(archive: ByteArray) {
        val entries = extractEntries(archive)
        val manifest = parseManifest(entries)
        validateFormatVersion(manifest)
        val dataJson = extractAndVerifyDataJson(entries, manifest)
        validateDataPayload(dataJson)

        val root = JSONObject(dataJson)
        val images = parseAndVerifyImages(entries, manifest, root)

        runInTransaction {
            restorer.clearExistingData()
            restorer.restoreCategories(root.getJSONArray("categories"))
            restorer.restoreItems(root.getJSONArray("items"))
            if (root.has("scoreProfiles")) {
                restorer.restoreScoreProfiles(root.getJSONArray("scoreProfiles"))
            }
            restorer.restoreImages(images)
        }
        runPostImportMaintenance()
    }

    private fun extractEntries(archive: ByteArray): Map<String, ByteArray> =
        try {
            readEntriesFromArchive(archive)
        } catch (e: ZipException) {
            throw BackupException("Invalid backup archive: ${e.message}", e)
        } catch (e: IOException) {
            throw BackupException("Invalid backup archive: ${e.message}", e)
        }

    private fun parseManifest(entries: Map<String, ByteArray>): JSONObject {
        val manifestBytes =
            entries[MANIFEST_ENTRY]
                ?: throw BackupException("Backup archive is missing $MANIFEST_ENTRY")
        return try {
            JSONObject(manifestBytes.toString(Charsets.UTF_8))
        } catch (e: JSONException) {
            throw BackupException("Invalid manifest JSON: ${e.message}", e)
        }
    }

    private fun validateFormatVersion(manifest: JSONObject) {
        val version = manifest.optInt("formatVersion", -1)
        if (version != ARCHIVE_FORMAT_VERSION) {
            throw BackupException("Unsupported archive format version: $version")
        }
    }

    private fun extractAndVerifyDataJson(
        entries: Map<String, ByteArray>,
        manifest: JSONObject,
    ): String {
        val (dataBytes, expectedSha256) = resolveDataBytes(entries, manifest)
        if (expectedSha256.isNotBlank() && dataBytes.sha256() != expectedSha256) {
            throw BackupException("Data checksum mismatch")
        }
        return dataBytes.toString(Charsets.UTF_8)
    }

    private fun resolveDataBytes(
        entries: Map<String, ByteArray>,
        manifest: JSONObject,
    ): Pair<ByteArray, String> {
        val dataSpec =
            manifest.optJSONObject("data")
                ?: throw BackupException("Manifest missing data entry specification")
        val dataPath = dataSpec.optString("path", DATA_ENTRY)
        val dataBytes =
            entries[dataPath]
                ?: throw BackupException("Backup archive is missing data file: $dataPath")
        return dataBytes to dataSpec.optString("sha256", "")
    }

    private fun validateDataPayload(dataJson: String) {
        val validationResult = validator.validate(dataJson)
        if (!validationResult.isValid) {
            throw BackupException("Backup validation failed: ${validationResult.errors.first()}")
        }
    }

    private fun parseAndVerifyImages(
        entries: Map<String, ByteArray>,
        manifest: JSONObject,
        root: JSONObject,
    ): List<ItemImageEntity> {
        val itemCategoryMap = buildItemCategoryMap(root.getJSONArray("items"))
        val imagesArray = manifest.optJSONArray("images") ?: JSONArray()
        return (0 until imagesArray.length()).map { i ->
            val imgObj = imagesArray.getJSONObject(i)
            parseSingleImage(imgObj, entries, itemCategoryMap)
        }
    }

    private fun parseSingleImage(
        imgObj: JSONObject,
        entries: Map<String, ByteArray>,
        itemCategoryMap: Map<String, String>,
    ): ItemImageEntity {
        val imageId = imgObj.getString("id")
        val itemId = imgObj.getString("itemId")
        val rawAttrId = imgObj.getString("attributeId")
        val resolvedAttrId =
            BackupAttributeIdNormalizer.resolveOrThrow(
                rawAttrId,
                itemCategoryMap[itemId] ?: "",
            )
        val path = imgObj.optString("path", "$IMAGE_DIRECTORY$imageId")
        val expectedSha256 = imgObj.optString("sha256", "")
        val imageBytes =
            entries[path]
                ?: throw BackupException("Missing image file in archive: $path")
        if (expectedSha256.isNotBlank() && imageBytes.sha256() != expectedSha256) {
            throw BackupException("Image checksum mismatch for $path")
        }
        return ItemImageEntity(
            id = imageId,
            itemId = itemId,
            attributeId = resolvedAttrId,
            position = imgObj.optInt("position", 0),
            bytes = imageBytes,
            mimeType = imgObj.optString("mimeType").ifBlank { null },
            displayName = imgObj.optString("displayName").ifBlank { null },
            width = if (imgObj.has("width")) imgObj.getInt("width") else null,
            height = if (imgObj.has("height")) imgObj.getInt("height") else null,
            createdAt = imgObj.optLong("createdAt", 0L),
        )
    }
}

private fun readEntriesFromArchive(archive: ByteArray): Map<String, ByteArray> {
    val entries = mutableMapOf<String, ByteArray>()
    ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
        generateSequence { zip.nextEntry }
            .filterNot { it.isDirectory }
            .forEach { entry ->
                validateEntryName(entry.name)
                entries[entry.name] = zip.readBytes()
            }
    }
    return entries
}

private fun validateEntryName(name: String) {
    if (name.contains("..") || name.startsWith("/")) {
        throw BackupException("Illegal entry path in archive: $name")
    }
}

private fun buildItemCategoryMap(itemsArray: JSONArray): Map<String, String> =
    (0 until itemsArray.length()).associate {
        val itemObj = itemsArray.getJSONObject(it)
        itemObj.getString("id") to itemObj.optString("categoryName", "")
    }
