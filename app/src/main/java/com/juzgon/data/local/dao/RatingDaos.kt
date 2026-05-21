package com.juzgon.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import kotlinx.coroutines.flow.Flow

data class CategoryWithAttributes(
    @Embedded
    val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "category_id",
    )
    val attributes: List<AttributeEntity>,
)

data class CategorySummary(
    @Embedded
    val category: CategoryEntity,
    @ColumnInfo(name = "item_count")
    val itemCount: Int,
    @ColumnInfo(name = "attribute_count")
    val attributeCount: Int,
)

data class ItemWithRatingsAndValues(
    @Embedded
    val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "item_id",
    )
    val ratings: List<RatingEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "item_id",
    )
    val values: List<ItemValueEntity>,
)

data class RankedItemWithRatings(
    @Embedded
    val item: ItemEntity,
    @ColumnInfo(name = "aggregate_score")
    val aggregateScore: Double,
    @Relation(
        parentColumn = "id",
        entityColumn = "item_id",
    )
    val ratings: List<RatingEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "item_id",
    )
    val values: List<ItemValueEntity>,
)

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsertCategory(category: CategoryEntity)

    @Upsert
    suspend fun upsertAttributes(attributes: List<AttributeEntity>)

    @Transaction
    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryWithAttributes(id: String): CategoryWithAttributes?

    @Transaction
    @Query("SELECT * FROM categories WHERE name = :name")
    fun getCategoryWithAttributesByName(name: String): CategoryWithAttributes?

    @Transaction
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeCategoriesWithAttributes(): Flow<List<CategoryWithAttributes>>

    @Transaction
    @Query("""
        SELECT c.*, 
               (SELECT COUNT(*) FROM items i WHERE i.category_id = c.id) as item_count,
               (SELECT COUNT(*) FROM attributes a WHERE a.category_id = c.id) as attribute_count
        FROM categories c
        ORDER BY c.name
    """)
    fun observeCategorySummaries(): Flow<List<CategorySummary>>

    @Transaction
    @Query("SELECT * FROM categories WHERE id = :id")
    fun observeCategoryWithAttributes(id: String): Flow<CategoryWithAttributes?>

    @Query("SELECT * FROM attributes")
    fun observeAttributes(): Flow<List<AttributeEntity>>

    @Query("SELECT * FROM attributes WHERE category_id = :categoryId")
    fun getAttributesForCategory(categoryId: String): List<AttributeEntity>

    @Query("DELETE FROM attributes WHERE category_id = :categoryId")
    suspend fun deleteAttributesForCategory(categoryId: String)

    @Query("DELETE FROM attributes WHERE category_id = :categoryId AND id NOT IN (:attributeIds)")
    suspend fun deleteAttributesNotIn(
        categoryId: String,
        attributeIds: List<String>,
    )

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)
}

@Dao
interface ItemDao {
    @Upsert
    suspend fun upsertItem(item: ItemEntity)

    @Upsert
    suspend fun upsertRatings(ratings: List<RatingEntity>)

    @Upsert
    suspend fun upsertValues(values: List<ItemValueEntity>)

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemWithRatingsAndValues(id: String): ItemWithRatingsAndValues?

    @Transaction
    @Query("SELECT * FROM items ORDER BY id")
    fun observeItemsWithRatingsAndValues(): Flow<List<ItemWithRatingsAndValues>>

    @Transaction
    @Query("SELECT * FROM items WHERE category_id = :categoryId ORDER BY name")
    fun observeItemsForCategory(categoryId: String): Flow<List<ItemWithRatingsAndValues>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun observeItemWithRatingsAndValues(id: String): Flow<ItemWithRatingsAndValues?>

    @Transaction
    @Query(
        """
        SELECT
            items.id AS id,
            items.category_id AS category_id,
            items.name AS name,
            items.notes AS notes,
            items.created_at AS created_at,
            items.updated_at AS updated_at,
            COALESCE(ROUND(SUM(ratings.score * attributes.weight) / SUM(attributes.weight), 1), 0.0) AS aggregate_score
        FROM items
        LEFT JOIN ratings ON ratings.item_id = items.id
        LEFT JOIN attributes ON attributes.id = ratings.attribute_id AND attributes.type = 'RATING'
        WHERE items.category_id = :categoryId
        GROUP BY items.id
        ORDER BY aggregate_score DESC, items.id ASC
        """,
    )
    fun observeRankedItemsForCategory(categoryId: String): Flow<List<RankedItemWithRatings>>

    @Query("SELECT * FROM ratings WHERE item_id = :itemId")
    fun getRatingsForItem(itemId: String): List<RatingEntity>

    @Query("SELECT * FROM item_values WHERE item_id = :itemId")
    fun getValuesForItem(itemId: String): List<ItemValueEntity>

    @Query("DELETE FROM ratings WHERE item_id = :itemId")
    suspend fun deleteRatingsForItem(itemId: String)

    @Query("DELETE FROM item_values WHERE item_id = :itemId")
    suspend fun deleteValuesForItem(itemId: String)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: String)
}
