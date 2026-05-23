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
}
