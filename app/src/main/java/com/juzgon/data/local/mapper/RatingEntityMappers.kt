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
import com.juzgon.domain.CatalogType
import com.juzgon.domain.Category
import com.juzgon.domain.ItemAttributeValue
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry
import com.juzgon.domain.ScoringDirection
import timber.log.Timber

fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        name = name,
        description = description,
        type = type?.name,
    )

fun Category.toAttributeEntities(): List<AttributeEntity> =
    attributes.mapIndexed { index, attribute ->
        AttributeEntity(
            id = attribute.id,
            categoryName = name,
            weight = attribute.weight,
            position = index,
            type = attribute.type.name,
            isRequired = attribute.isRequired,
            displayInDiamond = attribute.type == AttributeType.NUMBER && attribute.displayInDiamond,
            diamondOrder = attribute.diamondOrder,
            scoringDirection = attribute.scoringDirection?.name,
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
        description = description,
        type =
            type?.let { typeName ->
                CatalogType.entries.find { it.name == typeName }
            },
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
fun ItemWithRatings.toDomain(attributesById: Map<String, Attribute>): RatedItem =
    item.toDomain(ratings, attributesById, values.filter { it.deletedAt == null })

fun AttributeEntity.toDomain(): Attribute {
    val parsedType =
        runCatching { AttributeType.valueOf(type) }.getOrElse {
            Timber.w("Unknown attribute type '%s' for attribute '%s', defaulting to NUMBER", type, id)
            AttributeType.NUMBER
        }
    val parsedDirection =
        scoringDirection?.let { direction ->
            runCatching { ScoringDirection.valueOf(direction) }.getOrElse {
                Timber.w("Unknown scoring direction '%s' for attribute '%s'", direction, id)
                null
            }
        }
    return Attribute(
        id = id,
        weight = weight,
        type = parsedType,
        isRequired = isRequired,
        displayInDiamond = parsedType == AttributeType.NUMBER && displayInDiamond,
        diamondOrder = diamondOrder,
        scoringDirection = if (parsedType == AttributeType.DATE) parsedDirection else null,
    )
}
