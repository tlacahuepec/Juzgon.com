package com.juzgon.data.repository

import androidx.room.withTransaction
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.dao.ItemWithRatingsAndValues
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.data.local.mapper.toAttributeEntities
import com.juzgon.data.local.mapper.toDomain
import com.juzgon.data.local.mapper.toEntity
import com.juzgon.data.local.mapper.toItemEntity
import com.juzgon.data.local.mapper.toRatingEntities
import com.juzgon.data.local.mapper.toValueEntities
import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.usecase.RankRatedItemsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(
    private val database: JuzgonDatabase,
) : CategoryRepository {
    private val categoryDao = database.categoryDao()

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao
            .observeCategorySummaries()
            .map { summaries ->
                summaries.map { it.toDomain() }
            }.distinctUntilChanged()

    override fun observeCategory(id: String): Flow<Category?> =
        categoryDao
            .observeCategoryWithAttributes(id)
            .map { category ->
                category?.toDomain()
            }.distinctUntilChanged()

    override suspend fun saveCategory(category: Category) {
        database.withTransaction {
            categoryDao.upsertCategory(category.toEntity())
            if (category.attributes.isEmpty()) {
                categoryDao.deleteAttributesForCategory(category.id)
            } else {
                categoryDao.deleteAttributesNotIn(
                    categoryId = category.id,
                    attributeIds = category.attributes.map { it.id },
                )
                categoryDao.upsertAttributes(category.toAttributeEntities())
            }
        }
    }

    override suspend fun renameCategory(
        originalId: String,
        category: Category,
    ) {
        // Obsolete as we use UUIDs, keeping for interface compatibility
        database.withTransaction {
            require(categoryDao.getCategoryWithAttributesByName(category.name) == null || categoryDao.getCategoryWithAttributesByName(category.name)?.category?.id == category.id) {
                "Category name already exists"
            }
            saveCategory(category)
        }
    }

    override suspend fun deleteCategory(id: String) {
        database.withTransaction {
            categoryDao.deleteCategoryById(id)
        }
    }
}

class RoomRatedItemRepository(
    private val database: JuzgonDatabase,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) : RatedItemRepository {
    private val categoryDao = database.categoryDao()
    private val itemDao = database.itemDao()
    private val rankRatedItemsUseCase = RankRatedItemsUseCase()

    override fun observeRatedItems(): Flow<List<RatedItem>> =
        combine(
            itemDao.observeItemsWithRatingsAndValues(),
            categoryDao.observeAttributes(),
        ) { items, attributes ->
            val attributesById = attributes.associate { it.id to it.toDomain() }
            items.map { item -> item.toDomain(attributesById) }
        }.distinctUntilChanged()

    override fun observeRatedItem(id: String): Flow<RatedItem?> =
        combine(
            itemDao.observeItemWithRatingsAndValues(id),
            categoryDao.observeAttributes(),
        ) { item, attributes ->
            val attributesById = attributes.associate { it.id to it.toDomain() }
            item?.toDomain(attributesById)
        }.distinctUntilChanged()

    override fun observeRankedItems(categoryId: String): Flow<List<RankedRatedItem>> =
        combine(
            itemDao.observeRankedItemsForCategory(categoryId),
            categoryDao.observeAttributes(),
        ) { rankedItems, attributes ->
            val categoryAttributes = attributes.filter { it.categoryId == categoryId }
            if (categoryAttributes.isEmpty()) {
                emptyList()
            } else {
                val attributesById = categoryAttributes.associate { it.id to it.toDomain() }
                val ratingSystem = RatingSystem(attributesById.values.toList())
                val ratedItems =
                    rankedItems.map { rankedItem ->
                        rankedItem.item.toDomain(
                            ratings = rankedItem.ratings.filter { it.attributeId in attributesById },
                            values = rankedItem.values.filter { it.attributeId in attributesById },
                            attributesById = attributesById,
                        )
                    }
                rankRatedItemsUseCase(ratingSystem, ratedItems)
            }
        }.distinctUntilChanged()

    override suspend fun saveRatedItem(ratedItem: RatedItem) {
        database.withTransaction {
            val existingItem = itemDao.getItemWithRatingsAndValues(ratedItem.id)
            if (existingItem?.hasSameSavedContent(ratedItem) == true) {
                return@withTransaction
            }

            val updatedAt = currentTimeMillis()
            itemDao.upsertItem(
                ratedItem.toItemEntity(
                    createdAt = existingItem?.item?.createdAt ?: updatedAt,
                    updatedAt = updatedAt,
                ),
            )
            itemDao.deleteRatingsForItem(ratedItem.id)
            itemDao.deleteValuesForItem(ratedItem.id)
            
            val ratingEntities = ratedItem.toRatingEntities()
            if (ratingEntities.isNotEmpty()) {
                itemDao.upsertRatings(ratingEntities)
            }
            
            val valueEntities = ratedItem.toValueEntities()
            if (valueEntities.isNotEmpty()) {
                itemDao.upsertValues(valueEntities)
            }
        }
    }

    override suspend fun deleteRatedItem(id: String) {
        database.withTransaction {
            itemDao.deleteItemById(id)
        }
    }

    private fun ItemWithRatingsAndValues.hasSameSavedContent(ratedItem: RatedItem): Boolean =
        item.notes == ratedItem.notes &&
            item.name == ratedItem.name &&
            ratings.toRatingSnapshot() == ratedItem.toRatingEntities().toRatingSnapshot() &&
            values.toValueSnapshot() == ratedItem.toValueEntities().toValueSnapshot()

    private fun List<RatingEntity>.toRatingSnapshot(): List<Pair<String, Int>> =
        map { rating -> rating.attributeId to rating.score }
            .sortedBy { (attributeId, _) -> attributeId }
            
    private fun List<ItemValueEntity>.toValueSnapshot(): List<Pair<String, String>> =
        map { value -> value.attributeId to value.valueString }
            .sortedBy { (attributeId, _) -> attributeId }
}
