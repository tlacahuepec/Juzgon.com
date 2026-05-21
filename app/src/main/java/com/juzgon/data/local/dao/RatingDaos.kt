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
import com.juzgon.data.local.entity.RatingEntity
import kotlinx.coroutines.flow.Flow

data class CategoryItemCount(
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "item_count")
    val itemCount: Int,
)

data class CategoryWithAttributes(
    @Embedded
    val category: CategoryEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "category_name",
    )
    val attributes: List<AttributeEntity>,
)

data class ItemWithRatings(
    @Embedded
    val item: ItemEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "item_id",
    )
    val ratings: List<RatingEntity>,
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
)

@Dao
@Suppress("TooManyFunctions")
interface CategoryDao {
    @Upsert
    suspend fun upsertCategory(category: CategoryEntity)

    @Upsert
    suspend fun upsertAttributes(attributes: List<AttributeEntity>)

    @Transaction
    @Query("SELECT * FROM categories WHERE name = :name")
    fun getCategoryWithAttributes(name: String): CategoryWithAttributes?

    @Transaction
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeCategoriesWithAttributes(): Flow<List<CategoryWithAttributes>>

    @Transaction
    @Query("SELECT * FROM categories WHERE name = :name")
    fun observeCategoryWithAttributes(name: String): Flow<CategoryWithAttributes?>

    @Query("SELECT * FROM attributes")
    fun observeAttributes(): Flow<List<AttributeEntity>>

    @Query("SELECT * FROM attributes WHERE category_name = :categoryName")
    fun getAttributesForCategory(categoryName: String): List<AttributeEntity>

    @Query("DELETE FROM attributes WHERE category_name = :categoryName")
    suspend fun deleteAttributesForCategory(categoryName: String)

    @Query("DELETE FROM attributes WHERE category_name = :categoryName AND id NOT IN (:attributeIds)")
    suspend fun deleteAttributesNotIn(
        categoryName: String,
        attributeIds: List<String>,
    )

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategoryByName(name: String)

    @Query(
        """
        SELECT a.category_name, COUNT(DISTINCT r.item_id) AS item_count
        FROM attributes a
        LEFT JOIN ratings r ON r.attribute_id = a.id
        GROUP BY a.category_name
        """,
    )
    fun observeItemCountsByCategory(): Flow<List<CategoryItemCount>>
}

@Dao
interface ItemDao {
    @Upsert
    suspend fun upsertItem(item: ItemEntity)

    @Upsert
    suspend fun upsertRatings(ratings: List<RatingEntity>)

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemWithRatings(id: String): ItemWithRatings?

    @Transaction
    @Query("SELECT * FROM items ORDER BY id")
    fun observeItemsWithRatings(): Flow<List<ItemWithRatings>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun observeItemWithRatings(id: String): Flow<ItemWithRatings?>

    @Transaction
    @Query(
        """
        SELECT
            items.id AS id,
            items.notes AS notes,
            items.created_at AS created_at,
            items.updated_at AS updated_at,
            ROUND(SUM(ratings.score * attributes.weight) / SUM(attributes.weight), 1) AS aggregate_score
        FROM items
        INNER JOIN ratings ON ratings.item_id = items.id
        INNER JOIN attributes ON attributes.id = ratings.attribute_id
        WHERE attributes.category_name = :categoryName
        GROUP BY items.id
        ORDER BY aggregate_score DESC, items.id ASC
        """,
    )
    fun observeRankedItemsForCategory(categoryName: String): Flow<List<RankedItemWithRatings>>

    @Query("SELECT * FROM ratings WHERE item_id = :itemId")
    fun getRatingsForItem(itemId: String): List<RatingEntity>

    @Query("DELETE FROM ratings WHERE item_id = :itemId")
    suspend fun deleteRatingsForItem(itemId: String)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: String)
}
