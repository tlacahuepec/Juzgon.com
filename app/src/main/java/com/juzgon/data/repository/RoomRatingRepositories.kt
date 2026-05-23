package com.juzgon.data.repository

import androidx.room.withTransaction
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.dao.ItemWithRatings
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.data.local.mapper.toAttributeEntities
import com.juzgon.data.local.mapper.toDomain
import com.juzgon.data.local.mapper.toEntity
import com.juzgon.data.local.mapper.toItemEntity
import com.juzgon.data.local.mapper.toItemValueEntities
import com.juzgon.data.local.mapper.toRatingEntities
import com.juzgon.domain.AppClock
import com.juzgon.domain.AttributeRankSnapshot
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.DateScoreCalculator
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import com.juzgon.domain.RatingSystem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.buildAttributeRankSnapshots
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
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
        combine(
            categoryDao.observeCategoriesWithAttributes(),
            categoryDao.observeItemCountsByCategory(),
        ) { categories, itemCounts ->
            val countsByName = itemCounts.associate { it.categoryName to it.itemCount }
            categories.map { it.toDomain(itemCount = countsByName[it.category.name] ?: 0) }
        }.distinctUntilChanged()

    override fun observeCategory(name: String): Flow<Category?> =
        categoryDao
            .observeCategoryWithAttributes(name)
            .map { category ->
                category?.toDomain()
            }.distinctUntilChanged()

    override suspend fun saveCategory(category: Category) {
        saveCategory(category = category, renamedAttributeIds = emptyMap())
    }

    private suspend fun saveCategory(
        category: Category,
        renamedAttributeIds: Map<String, String>,
    ) {
        database.withTransaction {
            categoryDao.upsertCategory(category.toEntity())
            if (category.attributes.isEmpty()) {
                categoryDao.deleteAttributesForCategory(category.name)
            } else {
                categoryDao.upsertAttributes(category.toAttributeEntities())
                applyAttributeRenames(renamedAttributeIds)
                categoryDao.deleteAttributesNotIn(
                    categoryName = category.name,
                    attributeIds = category.attributes.map { it.id },
                )
            }
        }
    }

    override suspend fun renameCategory(
        originalName: String,
        category: Category,
        renamedAttributeIds: Map<String, String>,
    ) {
        if (originalName == category.name) {
            saveCategory(
                category = category,
                renamedAttributeIds = renamedAttributeIds,
            )
            return
        }

        database.withTransaction {
            require(categoryDao.getCategoryWithAttributes(category.name) == null) {
                "Category name already exists"
            }
            categoryDao.upsertCategory(category.toEntity())
            categoryDao.upsertAttributes(category.toAttributeEntities())
            applyAttributeRenames(renamedAttributeIds)
            categoryDao.deleteCategoryByName(originalName)
        }
    }

    override suspend fun deleteCategory(name: String) {
        database.withTransaction {
            categoryDao.deleteCategoryByName(name)
        }
    }

    private suspend fun applyAttributeRenames(renamedAttributeIds: Map<String, String>) {
        renamedAttributeIds.forEach { (oldAttributeId, newAttributeId) ->
            if (oldAttributeId == newAttributeId) {
                return@forEach
            }
            categoryDao.renameAttributeIdInRatings(
                oldAttributeId = oldAttributeId,
                newAttributeId = newAttributeId,
            )
            categoryDao.renameAttributeIdInItemValues(
                oldAttributeId = oldAttributeId,
                newAttributeId = newAttributeId,
            )
            categoryDao.renameAttributeIdInRankSnapshots(
                oldAttributeId = oldAttributeId,
                newAttributeId = newAttributeId,
            )
        }
    }
}

class RoomAttributeRankSnapshotRepository(
    database: JuzgonDatabase,
) : AttributeRankSnapshotRepository {
    private val snapshotDao = database.attributeRankSnapshotDao()

    override fun observeSnapshotsForItem(itemId: String): Flow<List<AttributeRankSnapshot>> =
        snapshotDao
            .observeSnapshotsForItem(itemId)
            .map { snapshots -> snapshots.map { it.toDomain() } }
            .distinctUntilChanged()
}

class RoomRatedItemRepository(
    private val database: JuzgonDatabase,
    private val dateScoreCalculator: DateScoreCalculator = DateScoreCalculator(AppClock { java.time.LocalDate.now() }),
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) : RatedItemRepository {
    private val categoryDao = database.categoryDao()
    private val itemDao = database.itemDao()
    private val snapshotDao = database.attributeRankSnapshotDao()
    private val rankRatedItemsUseCase = RankRatedItemsUseCase()

    override fun observeRatedItems(): Flow<List<RatedItem>> =
        combine(
            itemDao.observeItemsWithRatings(),
            categoryDao.observeAttributes(),
        ) { items, attributes ->
            val attributesById = attributes.associate { it.id to it.toDomain() }
            items.map { item -> item.toDomain(attributesById) }
        }.distinctUntilChanged()

    override fun observeRatedItem(id: String): Flow<RatedItem?> =
        combine(
            itemDao.observeItemWithRatings(id),
            categoryDao.observeAttributes(),
        ) { item, attributes ->
            val attributesById = attributes.associate { it.id to it.toDomain() }
            item?.toDomain(attributesById)
        }.distinctUntilChanged()

    override fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>> =
        combine(
            itemDao.observeRankedItemsForCategory(categoryName),
            categoryDao.observeAttributes(),
        ) { rankedItems, attributes ->
            val categoryAttributes = attributes.filter { it.categoryName == categoryName }
            val attributesById = categoryAttributes.associate { it.id to it.toDomain() }
            val rankableAttributes =
                attributesById.values.filter { attribute -> attribute.isRankable }
            if (rankableAttributes.isEmpty()) {
                emptyList()
            } else {
                val numberAttributeIds =
                    categoryAttributes.filter { it.type == "NUMBER" }.map { it.id }.toSet()
                val ratingSystem = RatingSystem(rankableAttributes)
                val ratedItems =
                    rankedItems.map { rankedItem ->
                        val baseItem =
                            rankedItem.item.toDomain(
                                ratings = rankedItem.ratings.filter { it.attributeId in numberAttributeIds },
                                attributesById = attributesById,
                                valueEntities = rankedItem.values.filter { it.attributeId in attributesById },
                            )
                        val dateScores =
                            rankableAttributes
                                .filter { it.type == AttributeType.DATE }
                                .mapNotNull { attribute ->
                                    val dateValue =
                                        baseItem.values
                                            .firstOrNull { it.attribute.id == attribute.id }
                                            ?.value
                                            ?: return@mapNotNull null
                                    val direction = attribute.scoringDirection ?: return@mapNotNull null
                                    val score =
                                        dateScoreCalculator.calculate(dateValue, direction)
                                            ?: return@mapNotNull null
                                    ScoreEntry(attribute = attribute, score = score)
                                }
                        baseItem.copy(scores = baseItem.scores + dateScores)
                    }
                rankRatedItemsUseCase(ratingSystem, ratedItems)
            }
        }.distinctUntilChanged()

    override suspend fun saveRatedItem(ratedItem: RatedItem) {
        database.withTransaction {
            val existingItemWithRatings = itemDao.getItemWithRatings(ratedItem.id)
            val ratingEntities = ratedItem.toRatingEntities()
            if (existingItemWithRatings?.hasSameSavedContent(ratedItem) == true) {
                return@withTransaction
            }

            val updatedAt = currentTimeMillis()
            val shouldSnapshotRanks =
                existingItemWithRatings == null ||
                    existingItemWithRatings.ratings.toRatingSnapshot() != ratingEntities.toRatingSnapshot()
            itemDao.upsertItem(
                ratedItem.toItemEntity(
                    createdAt = existingItemWithRatings?.item?.createdAt ?: updatedAt,
                    updatedAt = updatedAt,
                ),
            )
            itemDao.deleteRatingsForItem(ratedItem.id)
            if (ratingEntities.isNotEmpty()) {
                itemDao.upsertRatings(ratingEntities)
            }
            if (shouldSnapshotRanks) {
                val snapshots = buildAttributeRankSnapshots(ratedItem.id, updatedAt, ratedItem.scores)
                if (snapshots.isNotEmpty()) {
                    snapshotDao.upsertSnapshots(snapshots.map { it.toEntity() })
                }
            }
            itemDao.deleteItemValuesForItem(ratedItem.id)
            val valueEntities = ratedItem.toItemValueEntities()
            if (valueEntities.isNotEmpty()) {
                itemDao.upsertItemValues(valueEntities)
            }
        }
    }

    override suspend fun renameRatedItem(
        originalId: String,
        ratedItem: RatedItem,
    ) {
        if (originalId == ratedItem.id) {
            saveRatedItem(ratedItem)
            return
        }

        database.withTransaction {
            require(itemDao.getItemWithRatings(ratedItem.id) == null) {
                "Item already exists"
            }
            val existingItemWithRatings =
                checkNotNull(itemDao.getItemWithRatings(originalId)) {
                    "Item not found"
                }
            val ratingEntities = ratedItem.toRatingEntities()
            val updatedAt = currentTimeMillis()
            val shouldSnapshotRanks =
                existingItemWithRatings.ratings.toRatingSnapshot() != ratingEntities.toRatingSnapshot()

            itemDao.upsertItem(
                ratedItem.toItemEntity(
                    createdAt = existingItemWithRatings.item.createdAt,
                    updatedAt = updatedAt,
                ),
            )
            if (ratingEntities.isNotEmpty()) {
                itemDao.upsertRatings(ratingEntities)
            }
            val valueEntities = ratedItem.toItemValueEntities()
            if (valueEntities.isNotEmpty()) {
                itemDao.upsertItemValues(valueEntities)
            }
            val copiedSnapshots =
                snapshotDao
                    .getSnapshotsForItem(originalId)
                    .map { snapshot -> snapshot.copy(itemId = ratedItem.id) }
            if (copiedSnapshots.isNotEmpty()) {
                snapshotDao.upsertSnapshots(copiedSnapshots)
            }
            if (shouldSnapshotRanks) {
                val snapshots = buildAttributeRankSnapshots(ratedItem.id, updatedAt, ratedItem.scores)
                if (snapshots.isNotEmpty()) {
                    snapshotDao.upsertSnapshots(snapshots.map { it.toEntity() })
                }
            }
            itemDao.deleteItemById(originalId)
        }
    }

    override suspend fun deleteRatedItem(id: String) {
        database.withTransaction {
            itemDao.deleteItemById(id)
        }
    }

    private fun ItemWithRatings.hasSameSavedContent(ratedItem: RatedItem): Boolean =
        item.notes == ratedItem.notes &&
            ratings.toRatingSnapshot() == ratedItem.toRatingEntities().toRatingSnapshot() &&
            values.toValueSnapshot() == ratedItem.toItemValueEntities().toValueSnapshot()

    private fun List<RatingEntity>.toRatingSnapshot(): List<Pair<String, Int>> =
        map { rating -> rating.attributeId to rating.score }
            .sortedBy { (attributeId, _) -> attributeId }

    private fun List<ItemValueEntity>.toValueSnapshot(): List<Pair<String, String>> =
        map { it.attributeId to it.valueText }
            .sortedBy { (attributeId, _) -> attributeId }
}
