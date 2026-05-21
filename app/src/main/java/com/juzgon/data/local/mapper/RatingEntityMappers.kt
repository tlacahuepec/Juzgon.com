package com.juzgon.data.local.mapper

import com.juzgon.data.local.dao.CategorySummary as CategorySummaryEntity
import com.juzgon.data.local.dao.CategoryWithAttributes
import com.juzgon.data.local.dao.ItemWithRatingsAndValues
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemValue
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry

fun Category.toEntity(): CategoryEntity = CategoryEntity(id = id, name = name)

fun Category.toAttributeEntities(): List<AttributeEntity> =
    attributes.mapIndexed { index, attribute ->
        AttributeEntity(
            id = attribute.id,
            categoryId = id,
            name = attribute.name,
            type = attribute.type.name,
            isRequired = attribute.isRequired,
            weight = attribute.weight,
            position = index,
            options = attribute.options.joinToString(","),
        )
    }

fun CategoryEntity.toDomain(attributes: List<AttributeEntity>, itemCount: Int = 0): Category =
    Category(
        id = id,
        name = name,
        attributes =
            attributes
                .sortedWith(compareBy<AttributeEntity> { it.position }.thenBy { it.id })
                .map { it.toDomain() },
        itemCount = itemCount,
    )

fun CategoryWithAttributes.toDomain(): Category = category.toDomain(attributes)

fun CategorySummaryEntity.toDomain(): Category = category.toDomain(emptyList(), itemCount)

fun RatedItem.toItemEntity(
    createdAt: Long = this.createdAt,
    updatedAt: Long = this.updatedAt,
): ItemEntity =
    ItemEntity(
        id = id,
        categoryId = categoryId,
        name = name,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun RatedItem.toRatingEntities(): List<RatingEntity> =
    scores.map { scoreEntry ->
        RatingEntity(itemId = id, attributeId = scoreEntry.attribute.id, score = scoreEntry.score)
    }

fun RatedItem.toValueEntities(): List<ItemValueEntity> =
    values.map { itemValue ->
        ItemValueEntity(itemId = id, attributeId = itemValue.attribute.id, valueString = itemValue.valueString)
    }

fun ItemEntity.toDomain(
    ratings: List<RatingEntity>,
    values: List<ItemValueEntity>,
    attributesById: Map<String, Attribute>,
): RatedItem {
    val scoreEntries =
        ratings.sortedBy { it.attributeId }.map { rating ->
            val attribute =
                checkNotNull(attributesById[rating.attributeId]) {
                    "Missing Attribute mapping for id=${rating.attributeId}"
                }
            ScoreEntry(attribute = attribute, score = rating.score)
        }
    val valueEntries =
        values.sortedBy { it.attributeId }.map { value ->
            val attribute =
                checkNotNull(attributesById[value.attributeId]) {
                    "Missing Attribute mapping for id=${value.attributeId}"
                }
            ItemValue(attribute = attribute, valueString = value.valueString)
        }
    return RatedItem(
        id = id,
        categoryId = categoryId,
        name = name,
        scores = scoreEntries,
        values = valueEntries,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun ItemWithRatingsAndValues.toDomain(attributesById: Map<String, Attribute>): RatedItem =
    item.toDomain(ratings, values, attributesById)

fun AttributeEntity.toDomain(): Attribute = Attribute(
    id = id, 
    name = name, 
    type = AttributeType.valueOf(type),
    isRequired = isRequired,
    weight = weight,
    options = if (options.isNotBlank()) options.split(",") else emptyList()
)
