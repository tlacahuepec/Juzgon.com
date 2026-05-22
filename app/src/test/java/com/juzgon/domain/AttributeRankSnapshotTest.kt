package com.juzgon.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AttributeRankSnapshotTest {
    @Test
    fun buildAttributeRankSnapshotsSortsValuesHighestFirst() {
        val snapshots =
            buildAttributeRankSnapshots(
                itemId = ITEM_ID,
                capturedAt = CAPTURED_AT,
                scores =
                    listOf(
                        ScoreEntry(attribute = Attribute("speed"), score = 8),
                        ScoreEntry(attribute = Attribute("control"), score = 10),
                        ScoreEntry(attribute = Attribute("stamina"), score = 6),
                    ),
            )

        assertEquals(listOf("control", "speed", "stamina"), snapshots.map { it.attributeId })
        assertEquals(listOf(1, 2, 3), snapshots.map { it.rank })
        assertEquals(listOf(10, 8, 6), snapshots.map { it.value })
    }

    @Test
    fun buildAttributeRankSnapshotsOrdersTiesByAttributeId() {
        val snapshots =
            buildAttributeRankSnapshots(
                itemId = ITEM_ID,
                capturedAt = CAPTURED_AT,
                scores =
                    listOf(
                        ScoreEntry(attribute = Attribute("speed"), score = 8),
                        ScoreEntry(attribute = Attribute("agility"), score = 8),
                    ),
            )

        assertEquals(listOf("agility", "speed"), snapshots.map { it.attributeId })
        assertEquals(listOf(1, 2), snapshots.map { it.rank })
    }

    @Test
    fun buildAttributeRankSnapshotsReturnsEmptyListWhenNoNumericScoresExist() {
        assertEquals(emptyList<AttributeRankSnapshot>(), buildAttributeRankSnapshots(ITEM_ID, CAPTURED_AT, emptyList()))
    }

    @Test
    fun buildAttributeRankSnapshotsIncludesItemIdAndCapturedAtOnEverySnapshot() {
        val snapshots =
            buildAttributeRankSnapshots(
                itemId = ITEM_ID,
                capturedAt = CAPTURED_AT,
                scores = listOf(ScoreEntry(attribute = Attribute("speed"), score = 8)),
            )

        assertEquals(ITEM_ID, snapshots.single().itemId)
        assertEquals(CAPTURED_AT, snapshots.single().capturedAt)
    }

    private companion object {
        const val ITEM_ID = "item-1"
        const val CAPTURED_AT = 1_500L
    }
}
