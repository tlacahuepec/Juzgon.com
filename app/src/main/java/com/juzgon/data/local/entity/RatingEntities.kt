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
    @ColumnInfo(name = "type")
    val type: String = "NUMBER",
    @ColumnInfo(name = "is_required")
    val isRequired: Boolean = true,
    @ColumnInfo(name = "display_in_diamond")
    val displayInDiamond: Boolean = true,
    @ColumnInfo(name = "diamond_order")
    val diamondOrder: Int? = null,
    @ColumnInfo(name = "scoring_direction")
    val scoringDirection: String? = null,
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

@Entity(
    tableName = "item_values",
    primaryKeys = ["item_id", "attribute_id"],
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["item_id"]), Index(value = ["attribute_id"]), Index(value = ["deleted_at"])],
)
data class ItemValueEntity(
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "attribute_id")
    val attributeId: String,
    @ColumnInfo(name = "value_text")
    val valueText: String,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
)

@Entity(
    tableName = "attribute_rank_snapshots",
    primaryKeys = ["item_id", "captured_at", "attribute_id"],
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
data class AttributeRankSnapshotEntity(
    @ColumnInfo(name = "item_id")
    val itemId: String,
    @ColumnInfo(name = "captured_at")
    val capturedAt: Long,
    @ColumnInfo(name = "attribute_id")
    val attributeId: String,
    @ColumnInfo(name = "value")
    val value: Int,
    @ColumnInfo(name = "rank")
    val rank: Int,
)
