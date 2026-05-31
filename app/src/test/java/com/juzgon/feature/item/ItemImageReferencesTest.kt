package com.juzgon.feature.item

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemImageReferencesTest {
    @Test
    fun decodeLegacyImageValueReturnsSingleImageReference() {
        val decoded = decodeItemImageReferences("content://images/roadster")

        assertEquals(1, decoded.size)
        assertEquals("content://images/roadster", decoded.single().sourceUri)
    }

    @Test
    fun encodeAndDecodeRoundTripPreservesImageMetadata() {
        val references =
            listOf(
                ItemImageReference(
                    id = "img-1",
                    sourceUri = "content://images/roadster",
                    thumbnailUri = "content://images/roadster-thumb",
                    mimeType = "image/png",
                    sizeBytes = 1024,
                    width = 800,
                    height = 600,
                    createdAt = 1234L,
                    displayName = "roadster.png",
                ),
            )

        val encoded = encodeItemImageReferences(references)
        val decoded = decodeItemImageReferences(encoded)

        assertEquals(references, decoded)
    }

    @Test
    fun malformedJsonFallsBackToLegacyUriHandling() {
        val decoded = decodeItemImageReferences("{not valid json")

        assertEquals(1, decoded.size)
        assertTrue(decoded.single().sourceUri.startsWith("{not valid json"))
    }

    @Test
    fun decodeEmptyStringReturnsEmptyList() {
        val decoded = decodeItemImageReferences("")

        assertTrue(decoded.isEmpty())
    }

    @Test
    fun decodeBlankStringReturnsEmptyList() {
        val decoded = decodeItemImageReferences("   ")

        assertTrue(decoded.isEmpty())
    }

    @Test
    fun encodeEmptyListReturnsEmptyString() {
        val encoded = encodeItemImageReferences(emptyList())

        assertEquals("", encoded)
    }

    @Test
    fun roundTripMultipleImagesPreservesOrderAndData() {
        val references =
            listOf(
                ItemImageReference(
                    id = "1",
                    sourceUri = "content://img/1",
                    thumbnailUri = "content://img/1-thumb",
                    mimeType = "image/jpeg",
                    sizeBytes = 2048,
                    width = 1024,
                    height = 768,
                    createdAt = 111L,
                    displayName = "one.jpg",
                ),
                ItemImageReference(
                    id = "2",
                    sourceUri = "content://img/2",
                    thumbnailUri = "content://img/2",
                    mimeType = null,
                    sizeBytes = null,
                    width = null,
                    height = null,
                    createdAt = 222L,
                    displayName = null,
                ),
            )

        val encoded = encodeItemImageReferences(references)
        val decoded = decodeItemImageReferences(encoded)

        assertEquals(references, decoded)
    }

    @Test
    fun buildImageReferenceCreatesCorrectIdAndDefaults() {
        val ref =
            buildImageReference(
                sourceUri = "content://test/photo",
                mimeType = "image/png",
                sizeBytes = 512L,
                width = 400,
                height = 300,
                displayName = "photo.png",
                createdAt = 999L,
            )

        assertEquals("999:${"content://test/photo".hashCode()}", ref.id)
        assertEquals("content://test/photo", ref.sourceUri)
        assertEquals("content://test/photo", ref.thumbnailUri)
        assertEquals("image/png", ref.mimeType)
        assertEquals(512L, ref.sizeBytes)
        assertEquals(400, ref.width)
        assertEquals(300, ref.height)
        assertEquals(999L, ref.createdAt)
        assertEquals("photo.png", ref.displayName)
    }

    @Test
    fun decodeV1FormatWithAllFields() {
        val v1 =
            "imgref:v1|id=img-42;src=content%3A%2F%2Fphoto%2F1;thumb=content%3A%2F%2Fphoto%2F1-thumb;mime=image%2Fjpeg;size=4096;w=1920;h=1080;created=1700000000;name=photo.jpg"

        val decoded = decodeItemImageReferences(v1)

        assertEquals(1, decoded.size)
        val ref = decoded.first()
        assertEquals("img-42", ref.id)
        assertEquals("content://photo/1", ref.sourceUri)
        assertEquals("content://photo/1-thumb", ref.thumbnailUri)
        assertEquals("image/jpeg", ref.mimeType)
        assertEquals(4096L, ref.sizeBytes)
        assertEquals(1920, ref.width)
        assertEquals(1080, ref.height)
        assertEquals(1700000000L, ref.createdAt)
        assertEquals("photo.jpg", ref.displayName)
    }

    @Test
    fun roundTripWithSpecialCharactersInUriAndName() {
        val original =
            listOf(
                ItemImageReference(
                    id = "special-1",
                    sourceUri = "content://images/roadster with spaces & symbols?.jpg",
                    thumbnailUri = "content://images/roadster with spaces & symbols?.jpg",
                    displayName = "Roadster (2024) & Friends.jpg",
                ),
            )

        val encoded = encodeItemImageReferences(original)
        val decoded = decodeItemImageReferences(encoded)

        assertEquals(original, decoded)
    }
}
