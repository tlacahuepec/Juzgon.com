package com.juzgon.data.local.mapper

import com.juzgon.data.local.entity.AttributeRankSnapshotEntity
import com.juzgon.domain.AttributeRankSnapshot

fun AttributeRankSnapshot.toEntity(): AttributeRankSnapshotEntity =
    AttributeRankSnapshotEntity(
        itemId = itemId,
        capturedAt = capturedAt,
        attributeId = attributeId,
        value = value,
        rank = rank,
    )

fun AttributeRankSnapshotEntity.toDomain(): AttributeRankSnapshot =
    AttributeRankSnapshot(
        itemId = itemId,
        capturedAt = capturedAt,
        attributeId = attributeId,
        value = value,
        rank = rank,
    )
