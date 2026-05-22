package com.juzgon.data.local.mapper

import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class RatingEntityMappersTest {
    @Test
    fun categoryRoundTrip_preservesDomainValuesWithStableAttributeOrder() {
        val category =
            Category(
                name = "Food",
                attributes =
                    listOf(
                        Attribute(id = "taste", weight = 1.5),
                        Attribute(id = "service", weight = 1.0),
                    ),
            )

        val entity = category.toEntity()
        val attributeEntities = category.toAttributeEntities()

        val mappedBack = entity.toDomain(attributeEntities)

        assertEquals(category, mappedBack)
    }

    @Test
    fun ratedItemRoundTrip_preservesDomainValuesWithStableScoreOrder() {
        val ratedItem =
            RatedItem(
                id = "item-1",
                scores =
                    listOf(
                        ScoreEntry(attribute = Attribute(id = "taste", weight = 1.5), score = 8),
                        ScoreEntry(attribute = Attribute(id = "service", weight = 1.0), score = 6),
                    ),
            )
        val expected =
            RatedItem(
                id = "item-1",
                scores =
                    listOf(
                        ScoreEntry(attribute = Attribute(id = "service", weight = 1.0), score = 6),
                        ScoreEntry(attribute = Attribute(id = "taste", weight = 1.5), score = 8),
                    ),
            )

        val itemEntity = ratedItem.toItemEntity()
        val ratingEntities = ratedItem.toRatingEntities()
        val attributesById = ratedItem.scores.associate { it.attribute.id to it.attribute }

        val mappedBack = itemEntity.toDomain(ratingEntities, attributesById)

        assertEquals(expected, mappedBack)
    }

    @Test
    fun categoryWithNoAttributes_mapsToEmptyAttributeEntityList() {
        val category = Category(name = "Empty", attributes = emptyList())

        val mappedAttributes = category.toAttributeEntities()
        val mappedBack = CategoryEntity(name = category.name).toDomain(mappedAttributes)

        assertEquals(emptyList<String>(), mappedAttributes.map { it.id })
        assertEquals(category, mappedBack)
    }

    @Test
    fun attributeEntity_defaultWeight_mapsToDomainDefault() {
        val attributeEntity = AttributeEntity(id = "price", categoryName = "Food")

        val mappedDomain = attributeEntity.toDomain()

        assertEquals(Attribute(id = "price", weight = 1.0), mappedDomain)
    }

    @Test
    fun attributeEntity_roundTrip_preservesDiamondChartConfiguration() {
        val category =
            Category(
                name = "Cars",
                attributes =
                    listOf(
                        Attribute(id = "speed", displayInDiamond = true, diamondOrder = 2),
                        Attribute(id = "photo", type = AttributeType.IMAGE),
                    ),
            )

        val mappedBack = category.toEntity().toDomain(category.toAttributeEntities())

        assertEquals(category, mappedBack)
    }

    @Test
    fun itemEntity_toDomain_withNoRatings_returnsEmptyScores() {
        val itemEntity = ItemEntity(id = "item-2")

        val mapped = itemEntity.toDomain(ratings = emptyList(), attributesById = emptyMap())

        assertEquals(RatedItem(id = "item-2", scores = emptyList()), mapped)
    }

    @Test(expected = IllegalStateException::class)
    fun itemEntity_toDomain_throwsWhenRatingAttributeMissing() {
        val itemEntity = ItemEntity(id = "item-3")

        itemEntity.toDomain(
            ratings = listOf(RatingEntity(itemId = "item-3", attributeId = "unknown", score = 5)),
            attributesById = emptyMap(),
        )
    }
}
