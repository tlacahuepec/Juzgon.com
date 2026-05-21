package com.juzgon.data.local

import androidx.room.withTransaction
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.domain.AttributeType

internal const val SAMPLE_CATEGORY_ID = "sample-category-123"
internal const val SAMPLE_CATEGORY_NAME = "Restaurants"
internal const val SAMPLE_ITEM_ID = "sample-item-123"

private const val TASTE_ATTRIBUTE_ID = "taste"
private const val SERVICE_ATTRIBUTE_ID = "service"
private const val AMBIENCE_ATTRIBUTE_ID = "ambience"

internal class RoomSampleDataStore(
    private val database: JuzgonDatabase,
) : SampleDataStore {
    private val categoryDao = database.categoryDao()
    private val itemDao = database.itemDao()

    override suspend fun seed() {
        database.withTransaction {
            if (categoryDao.getCategoryWithAttributesByName(SAMPLE_CATEGORY_NAME) != null) {
                return@withTransaction
            }

            categoryDao.upsertCategory(CategoryEntity(id = SAMPLE_CATEGORY_ID, name = SAMPLE_CATEGORY_NAME))
            categoryDao.upsertAttributes(sampleAttributes())
            itemDao.upsertItem(ItemEntity(id = SAMPLE_ITEM_ID, categoryId = SAMPLE_CATEGORY_ID, name = "Sample bistro"))
            itemDao.upsertRatings(sampleRatings())
        }
    }

    private fun sampleAttributes(): List<AttributeEntity> =
        listOf(
            AttributeEntity(
                id = TASTE_ATTRIBUTE_ID,
                categoryId = SAMPLE_CATEGORY_ID,
                name = "Taste",
                type = AttributeType.RATING.name,
                weight = 1.5,
                position = 0,
            ),
            AttributeEntity(
                id = SERVICE_ATTRIBUTE_ID,
                categoryId = SAMPLE_CATEGORY_ID,
                name = "Service",
                type = AttributeType.RATING.name,
                weight = 1.0,
                position = 1,
            ),
            AttributeEntity(
                id = AMBIENCE_ATTRIBUTE_ID,
                categoryId = SAMPLE_CATEGORY_ID,
                name = "Ambience",
                type = AttributeType.RATING.name,
                weight = 1.0,
                position = 2,
            ),
        )

    private fun sampleRatings(): List<RatingEntity> =
        listOf(
            RatingEntity(
                itemId = SAMPLE_ITEM_ID,
                attributeId = TASTE_ATTRIBUTE_ID,
                score = 8,
            ),
            RatingEntity(
                itemId = SAMPLE_ITEM_ID,
                attributeId = SERVICE_ATTRIBUTE_ID,
                score = 9,
            ),
            RatingEntity(
                itemId = SAMPLE_ITEM_ID,
                attributeId = AMBIENCE_ATTRIBUTE_ID,
                score = 8,
            ),
        )
}
