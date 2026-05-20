package com.juzgon.data.local

internal class DebugSampleDataSeeder(
    private val isEnabled: Boolean,
    private val sampleDataStore: SampleDataStore,
) {
    suspend fun seed() {
        if (isEnabled) {
            sampleDataStore.seed()
        }
    }
}

internal interface SampleDataStore {
    suspend fun seed()
}
