package com.juzgon.data.local.mapper

import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.domain.Attribute
import com.juzgon.domain.Category
import com.juzgon.domain.RatedItem
import com.juzgon.domain.ScoreEntry

fun Category.toEntity(): CategoryEntity = CategoryEntity(name = name)

fun Category.toAttributeEntities(): List<AttributeEntity> =
    attributes.map { attributeId ->
        AttributeEntity(id = attributeId, categoryName = name)
    }

fun CategoryEntity.toDomain(attributes: List<AttributeEntity>): Category = Category(name = name, attributes = attributes.map { it.id })

fun RatedItem.toItemEntity(): ItemEntity = ItemEntity(id = id)

fun RatedItem.toRatingEntities(): List<RatingEntity> =
    scores.map { scoreEntry ->
        RatingEntity(itemId = id, attributeId = scoreEntry.attribute.id, score = scoreEntry.score)
    }

fun ItemEntity.toDomain(
    ratings: List<RatingEntity>,
    attributesById: Map<String, Attribute>,
): RatedItem {
    val scoreEntries =
        ratings.map { rating ->
            val attribute =
                checkNotNull(attributesById[rating.attributeId]) {
                    "Missing Attribute mapping for id=${rating.attributeId}"
                }
            ScoreEntry(attribute = attribute, score = rating.score)
        }
    return RatedItem(id = id, scores = scoreEntries)
}

fun AttributeEntity.toDomain(): Attribute = Attribute(id = id, weight = weight)
