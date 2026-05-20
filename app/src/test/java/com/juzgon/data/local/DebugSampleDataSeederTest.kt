package com.juzgon.data.local

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugSampleDataSeederTest {
    @Test
    fun seed_whenEnabled_runsSampleStore() =
        runTest {
            val sampleDataStore = RecordingSampleDataStore()

            DebugSampleDataSeeder(
                isEnabled = true,
                sampleDataStore = sampleDataStore,
            ).seed()

            assertEquals(1, sampleDataStore.seedCount)
        }

    @Test
    fun seed_whenDisabled_doesNotRunSampleStore() =
        runTest {
            val sampleDataStore = RecordingSampleDataStore()

            DebugSampleDataSeeder(
                isEnabled = false,
                sampleDataStore = sampleDataStore,
            ).seed()

            assertEquals(0, sampleDataStore.seedCount)
        }

    private class RecordingSampleDataStore : SampleDataStore {
        var seedCount = 0
            private set

        override suspend fun seed() {
            seedCount += 1
        }
    }
}
