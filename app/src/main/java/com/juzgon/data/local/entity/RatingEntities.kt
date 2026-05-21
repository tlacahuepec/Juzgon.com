package com.juzgon.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
)
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "name")
    val name: String,
)

@Entity(
    tableName = "attributes",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["name"],
            childColumns = ["category_name"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["category_name"])],
)
data class AttributeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "weight")
    val weight: Double = 1.0,
    @ColumnInfo(name = "position")
    val position: Int = 0,
)

@Entity(
    tableName = "items",
)
data class ItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "notes")
    val notes: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L,
)

@Entity(
    tableName = "ratings",
    primaryKeys = ["item_id", "attribute_id"],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttributeEntity::class,
            parentColumns = ["id"],
            childColumns = ["attribute_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["item_id"]), Index(value = ["attribute_id"])],
)
data class RatingEntity(
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "attribute_id")
    val attributeId: String,
    @ColumnInfo(name = "score")
    val score: Int,
)
