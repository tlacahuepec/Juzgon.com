package com.juzgon.data.local.mapper

import com.juzgon.data.local.dao.CategoryWithAttributes
import com.juzgon.data.local.dao.ItemWithRatings
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry

fun Category.toEntity(): CategoryEntity = CategoryEntity(name = name)

fun Category.toAttributeEntities(): List<AttributeEntity> =
    attributes.mapIndexed { index, attribute ->
        AttributeEntity(
            id = attribute.id,
            categoryName = name,
            weight = attribute.weight,
            position = index,
            type = attribute.type.name,
            isRequired = attribute.isRequired,
        )
    }

fun CategoryEntity.toDomain(
    attributes: List<AttributeEntity>,
    itemCount: Int = 0,
): Category =
    Category(
        name = name,
        attributes =
            attributes
                .sortedWith(compareBy<AttributeEntity> { it.position }.thenBy { it.id })
                .map { it.toDomain() },
        itemCount = itemCount,
    )

fun CategoryWithAttributes.toDomain(itemCount: Int = 0): Category = category.toDomain(attributes, itemCount)

fun RatedItem.toItemEntity(
    createdAt: Long = this.createdAt,
    updatedAt: Long = this.updatedAt,
): ItemEntity =
    ItemEntity(
        id = id,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun RatedItem.toRatingEntities(): List<RatingEntity> =
    scores.map { scoreEntry ->
        RatingEntity(itemId = id, attributeId = scoreEntry.attribute.id, score = scoreEntry.score)
    }

fun RatedItem.toItemValueEntities(): List<ItemValueEntity> =
    values.map { valueEntry ->
        ItemValueEntity(itemId = id, attributeId = valueEntry.attribute.id, valueText = valueEntry.value)
    }

fun ItemEntity.toDomain(
    ratings: List<RatingEntity>,
    attributesById: Map<String, Attribute>,
    valueEntities: List<ItemValueEntity> = emptyList(),
): RatedItem {
    val scoreEntries =
        ratings.sortedBy { it.attributeId }.map { rating ->
            val attribute =
                checkNotNull(attributesById[rating.attributeId]) {
                    "Missing Attribute mapping for id=${rating.attributeId}"
                }
            ScoreEntry(attribute = attribute, score = rating.score)
        }
    val values =
        valueEntities.sortedBy { it.attributeId }.mapNotNull { valueEntity ->
            attributesById[valueEntity.attributeId]?.let { attribute ->
                ItemAttributeValue(attribute = attribute, value = valueEntity.valueText)
            }
        }
    return RatedItem(
        id = id,
        scores = scoreEntries,
        notes = notes,
        values = values,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

@Suppress("MaxLineLength")
fun ItemWithRatings.toDomain(attributesById: Map<String, Attribute>): RatedItem = item.toDomain(ratings, attributesById, values)

fun AttributeEntity.toDomain(): Attribute =
    Attribute(
        id = id,
        weight = weight,
        type = runCatching { AttributeType.valueOf(type) }.getOrDefault(AttributeType.NUMBER),
        isRequired = isRequired,
    )
