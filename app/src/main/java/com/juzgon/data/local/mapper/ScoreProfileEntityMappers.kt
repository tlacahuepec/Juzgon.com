package com.juzgon.data.local.mapper

import com.juzgon.data.local.entity.ScoreProfileAttributeEntity
import com.juzgon.data.local.entity.ScoreProfileEntity
import com.juzgon.domain.ScoreProfile

fun ScoreProfileEntity.toDomain(attributeIds: List<String>): ScoreProfile =
    ScoreProfile(
        id = id,
        categoryName = categoryName,
        name = name,
        includedAttributeIds = attributeIds,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ScoreProfile.toEntity(): ScoreProfileEntity =
    ScoreProfileEntity(
        id = id,
        categoryName = categoryName,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ScoreProfile.toAttributeEntities(): List<ScoreProfileAttributeEntity> =
    includedAttributeIds.mapIndexed { index, attributeId ->
        ScoreProfileAttributeEntity(
            profileId = id,
            attributeId = attributeId,
            position = index,
        )
    }
