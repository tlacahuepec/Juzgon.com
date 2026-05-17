package com.juzgon.data.repository

import androidx.room.withTransaction
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.mapper.toAttributeEntities
import com.juzgon.data.local.mapper.toDomain
import com.juzgon.data.local.mapper.toEntity
import com.juzgon.data.local.mapper.toItemEntity
import com.juzgon.data.local.mapper.toRatingEntities
import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.usecase.RankRatedItemsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(
    private val database: JuzgonDatabase,
) : CategoryRepository {
    private val categoryDao = database.categoryDao()

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeCategoriesWithAttributes().map { categories ->
            categories.map { it.toDomain() }
        }

    override fun observeCategory(name: String): Flow<Category?> =
        categoryDao.observeCategoryWithAttributes(name).map { category ->
            category?.toDomain()
        }

    override suspend fun saveCategory(category: Category) {
        database.withTransaction {
            categoryDao.upsertCategory(category.toEntity())
            if (category.attributes.isEmpty()) {
                categoryDao.deleteAttributesForCategory(category.name)
            } else {
                categoryDao.deleteAttributesNotIn(
                    categoryName = category.name,
                    attributeIds = category.attributes,
                )
                categoryDao.upsertAttributes(category.toAttributeEntities())
            }
        }
    }

    override suspend fun deleteCategory(name: String) {
        database.withTransaction {
            categoryDao.deleteCategoryByName(name)
        }
    }
}

class RoomRatedItemRepository(
    private val database: JuzgonDatabase,
) : RatedItemRepository {
    private val categoryDao = database.categoryDao()
    private val itemDao = database.itemDao()
    private val rankRatedItemsUseCase = RankRatedItemsUseCase()

    override fun observeRatedItems(): Flow<List<RatedItem>> =
        combine(
            itemDao.observeItemsWithRatings(),
            categoryDao.observeAttributes(),
        ) { items, attributes ->
            val attributesById = attributes.associate { it.id to it.toDomain() }
            items.map { item -> item.toDomain(attributesById) }
        }

    override fun observeRatedItem(id: String): Flow<RatedItem?> =
        combine(
            itemDao.observeItemWithRatings(id),
            categoryDao.observeAttributes(),
        ) { item, attributes ->
            val attributesById = attributes.associate { it.id to it.toDomain() }
            item?.toDomain(attributesById)
        }

    override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> =
        combine(
            itemDao.observeRankedItemsForCategory(categoryName),
            categoryDao.observeAttributes(),
        ) { rankedItems, attributes ->
            val categoryAttributes = attributes.filter { it.categoryName == categoryName }
            if (categoryAttributes.isEmpty()) {
                emptyList()
            } else {
                val attributesById = categoryAttributes.associate { it.id to it.toDomain() }
                val ratingSystem = RatingSystem(attributesById.values.toList())
                val ratedItems =
                    rankedItems.map { rankedItem ->
                        rankedItem.item.toDomain(
                            ratings = rankedItem.ratings.filter { it.attributeId in attributesById },
                            attributesById = attributesById,
                        )
                    }
                rankRatedItemsUseCase(ratingSystem, ratedItems)
            }
        }

    override suspend fun saveRatedItem(ratedItem: RatedItem) {
        database.withTransaction {
            itemDao.upsertItem(ratedItem.toItemEntity())
            itemDao.deleteRatingsForItem(ratedItem.id)
            val ratingEntities = ratedItem.toRatingEntities()
            if (ratingEntities.isNotEmpty()) {
                itemDao.upsertRatings(ratingEntities)
            }
        }
    }

    override suspend fun deleteRatedItem(id: String) {
        database.withTransaction {
            itemDao.deleteItemById(id)
        }
    }
}
