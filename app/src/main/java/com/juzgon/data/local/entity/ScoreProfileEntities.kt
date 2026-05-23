package com.juzgon.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "score_profiles",
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
data class ScoreProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "category_name")
    val categoryName: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L,
)

@Entity(
    tableName = "score_profile_attributes",
    primaryKeys = ["profile_id", "attribute_id"],
    foreignKeys = [
        ForeignKey(
            entity = ScoreProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AttributeEntity::class,
            parentColumns = ["id"],
            childColumns = ["attribute_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["profile_id"]), Index(value = ["attribute_id"])],
)
data class ScoreProfileAttributeEntity(
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "attribute_id")
    val attributeId: String,
    @ColumnInfo(name = "position")
    val position: Int = 0,
)
