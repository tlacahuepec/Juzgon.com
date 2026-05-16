package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RatingModelsTest {
    @Test
    fun `rated item rejects duplicate attributes`() {
        val quality = Attribute(id = "quality")

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                RatedItem(
                    id = "item-1",
                    scores =
                        listOf(
                            ScoreEntry(attribute = quality, score = 8),
                            ScoreEntry(attribute = quality, score = 6),
                        ),
                )
            }

        assertEquals("Rated item scores must not contain duplicate attribute ids", error.message)
    }

    @Test
    fun `rating system rejects duplicate attribute ids`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                RatingSystem(
                    attributes =
                        listOf(
                            Attribute(id = "quality", weight = 2.0),
                            Attribute(id = "quality", weight = 1.0),
                        ),
                )
            }

        assertEquals("Rating system attributes must have unique ids", error.message)
    }
}
