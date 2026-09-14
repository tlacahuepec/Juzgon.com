package com.juzgon.data.backup

import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.dao.ScoreProfileAttributeDao
import com.juzgon.data.local.dao.ScoreProfileDao
import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupService
import com.juzgon.domain.backup.BackupValidator
import kotlinx.coroutines.flow.first
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("LongParameterList")
class JsonBackupService(
    private val validator: BackupValidator,
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val scoreProfileDao: ScoreProfileDao,
    private val scoreProfileAttributeDao: ScoreProfileAttributeDao,
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit,
    private val runPostImportMaintenance: suspend () -> Unit = {},
    private val serializer: JsonBackupSerializer = JsonBackupSerializer(),
    private val restorer: JsonBackupRestorer =
        JsonBackupRestorer(
            categoryDao,
            itemDao,
            scoreProfileDao,
            scoreProfileAttributeDao,
        ),
    private val archiveRestorer: JsonBackupArchiveRestorer =
        JsonBackupArchiveRestorer(
            validator,
            restorer,
            runInTransaction,
            runPostImportMaintenance,
        ),
) : BackupService {
    override suspend fun export(): String = exportSnapshot().json

    override suspend fun importArchive(archive: ByteArray) {
        archiveRestorer.restoreArchive(archive)
    }

    override suspend fun import(json: String) {
        val validationResult = validator.validate(json)
        if (!validationResult.isValid) {
            throw BackupException("Backup validation failed: ${validationResult.errors.first()}")
        }

        val root =
            try {
                JSONObject(json)
            } catch (e: JSONException) {
                throw BackupException("Invalid JSON: ${e.message}", e)
            }

        runInTransaction {
            restorer.clearExistingData()
            restorer.restoreCategories(root.getJSONArray("categories"))
            restorer.restoreItems(root.getJSONArray("items"))
            if (root.has("scoreProfiles")) {
                restorer.restoreScoreProfiles(root.getJSONArray("scoreProfiles"))
            }
        }

        runPostImportMaintenance()
    }

    /**
     * Exports the portable v1 ZIP contract:
     * - manifest.json describes every entry and binary image checksum.
     * - data.json contains the validated JSON catalog payload.
     * - images/<image id> contains the original stored image bytes.
     *
     * Entries and images are sorted and ZIP timestamps are fixed so the archive layout is stable.
     */
    override suspend fun exportArchive(): ByteArray {
        val snapshot = exportSnapshot()
        validateExportJson(snapshot.json)
        val data = JSONObject(snapshot.json)
        val archiveImages =
            snapshot.items
                .flatMap { item -> item.images.map { item.item.id to it } }
                .sortedWith(
                    compareBy<Pair<String, com.juzgon.data.local.entity.ItemImageEntity>> { it.second.id }
                        .thenBy { it.first },
                ).map { (itemId, image) -> image.toArchiveImage(itemId) }
        validateArchiveImages(archiveImages)

        val dataBytes = snapshot.json.toByteArray(Charsets.UTF_8)
        val manifest = buildManifest(data, dataBytes, archiveImages)
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.writeEntry(MANIFEST_ENTRY, manifest.toString().toByteArray(Charsets.UTF_8))
                zip.writeEntry(DATA_ENTRY, dataBytes)
                archiveImages.forEach { image -> zip.writeEntry(image.path, image.bytes) }
            }
            output.toByteArray()
        }
    }

    private suspend fun exportSnapshot(): ExportSnapshot {
        val categories = categoryDao.observeCategoriesWithAttributes().first()
        val items = itemDao.observeItemsWithRatings().first()
        val profiles = scoreProfileDao.observeAllProfiles().first()
        val profileAttributes = scoreProfileAttributeDao.observeAllProfileAttributes().first()
        return ExportSnapshot(
            json = serializer.serializeExport(categories, items, profiles, profileAttributes),
            items = items,
        )
    }

    private fun validateExportJson(json: String) {
        val validation = validator.validate(json)
        if (!validation.isValid) {
            throw BackupException("Backup validation failed: ${validation.errors.first()}")
        }
    }

    private fun buildManifest(
        data: JSONObject,
        dataBytes: ByteArray,
        images: List<ArchiveImage>,
    ): JSONObject =
        JSONObject()
            .put("formatVersion", ARCHIVE_FORMAT_VERSION)
            .put("app", data.getString("app"))
            .put("schemaVersion", data.getInt("version"))
            .put("exportedAt", data.getString("exportedAt"))
            .put("data", JSONObject().put("path", DATA_ENTRY).put("sha256", dataBytes.sha256()))
            .put(
                "images",
                org.json.JSONArray().apply {
                    images.forEach { image ->
                        put(
                            JSONObject()
                                .put("id", image.id)
                                .put("itemId", image.itemId)
                                .put("attributeId", image.attributeId)
                                .put("position", image.position)
                                .put("path", image.path)
                                .put("sha256", image.bytes.sha256())
                                .putOptional("mimeType", image.mimeType)
                                .putOptional("displayName", image.displayName)
                                .putOptional("width", image.width)
                                .putOptional("height", image.height)
                                .put("createdAt", image.createdAt),
                        )
                    }
                },
            )
            // Kept even when empty so consumers do not need to infer whether checks completed.
            .put("warnings", org.json.JSONArray())

    private fun validateArchiveImages(images: List<ArchiveImage>) {
        val ids = mutableSetOf<String>()
        images.forEach { image ->
            validateImageIdentifier(image.id, ids)
            validateImageMetadata(image)
        }
    }

    private fun com.juzgon.data.local.entity.ItemImageEntity.toArchiveImage(itemId: String): ArchiveImage =
        ArchiveImage(
            id = id,
            itemId = itemId,
            attributeId = attributeId,
            position = position,
            bytes = bytes,
            mimeType = mimeType,
            displayName = displayName,
            width = width,
            height = height,
            createdAt = createdAt,
        )

    private fun ZipOutputStream.writeEntry(
        name: String,
        bytes: ByteArray,
    ) {
        putNextEntry(ZipEntry(name).apply { time = 0L })
        write(bytes)
        closeEntry()
    }
}

private fun validateImageIdentifier(
    id: String,
    ids: MutableSet<String>,
) {
    if (!ids.add(id)) {
        throw BackupException("Backup contains duplicate image id: $id")
    }
    if (id.isBlank() || !id.matches(ARCHIVE_ENTRY_ID)) {
        throw BackupException("Image id cannot be represented safely in a backup archive: $id")
    }
}

private fun validateImageMetadata(image: ArchiveImage) {
    if (image.itemId.isBlank() || image.attributeId.isBlank() || image.position < 0) {
        throw BackupException("Image metadata is invalid for image: ${image.id}")
    }
    val invalidWidth = image.width != null && image.width <= 0
    val invalidHeight = image.height != null && image.height <= 0
    if (invalidWidth || invalidHeight) {
        throw BackupException("Image dimensions are invalid for image: ${image.id}")
    }
}

private val ARCHIVE_ENTRY_ID = Regex("[A-Za-z0-9._-]+")

private data class ExportSnapshot(
    val json: String,
    val items: List<com.juzgon.data.local.dao.ItemWithRatings>,
)

private data class ArchiveImage(
    val id: String,
    val itemId: String,
    val attributeId: String,
    val position: Int,
    val bytes: ByteArray,
    val mimeType: String?,
    val displayName: String?,
    val width: Int?,
    val height: Int?,
    val createdAt: Long,
) {
    val path: String = "$IMAGE_DIRECTORY$id"
}

private fun JSONObject.putOptional(
    name: String,
    value: Any?,
): JSONObject = apply { if (value != null) put(name, value) }

internal fun ByteArray.sha256(): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }
