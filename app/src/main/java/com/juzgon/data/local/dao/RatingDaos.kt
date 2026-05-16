package com.juzgon.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.RatingEntity

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

@Dao
interface CategoryDao {
    @Insert
    fun insertCategory(category: CategoryEntity)

    @Insert
    fun insertAttributes(attributes: List<AttributeEntity>)

    @Transaction
    @Query("SELECT * FROM categories WHERE name = :name")
    fun getCategoryWithAttributes(name: String): CategoryWithAttributes?

    @Query("SELECT * FROM attributes WHERE category_name = :categoryName")
    fun getAttributesForCategory(categoryName: String): List<AttributeEntity>

    @Delete
    fun deleteCategory(category: CategoryEntity)
}

@Dao
interface ItemDao {
    @Insert
    fun insertItem(item: ItemEntity)

    @Insert
    fun insertRatings(ratings: List<RatingEntity>)

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemWithRatings(id: String): ItemWithRatings?

    @Query("SELECT * FROM ratings WHERE item_id = :itemId")
    fun getRatingsForItem(itemId: String): List<RatingEntity>

    @Delete
    fun deleteItem(item: ItemEntity)
}
