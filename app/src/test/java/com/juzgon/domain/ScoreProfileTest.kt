package com.juzgon.domain

import org.junit.Test

class ScoreProfileTest {
    @Test(expected = IllegalArgumentException::class)
    fun `requires non-blank id`() {
        ScoreProfile(id = "", categoryName = "Food", name = "Physical", includedAttributeIds = listOf("taste"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requires non-blank categoryName`() {
        ScoreProfile(id = "p1", categoryName = "", name = "Physical", includedAttributeIds = listOf("taste"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requires non-blank name`() {
        ScoreProfile(id = "p1", categoryName = "Food", name = "", includedAttributeIds = listOf("taste"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requires non-empty includedAttributeIds`() {
        ScoreProfile(id = "p1", categoryName = "Food", name = "Physical", includedAttributeIds = emptyList())
    }

    @Test
    fun `valid ScoreProfile creates successfully`() {
        val profile =
            ScoreProfile(
                id = "p1",
                categoryName = "Food",
                name = "Physical Only",
                includedAttributeIds = listOf("taste", "texture"),
                createdAt = 1000L,
                updatedAt = 2000L,
            )
        assert(profile.id == "p1")
        assert(profile.includedAttributeIds.size == 2)
    }
}
