@file:Suppress("MaxLineLength")

package com.juzgon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.juzgon.data.local.entity.EnrichmentSuggestionCacheEntity

@Dao
interface EnrichmentSuggestionCacheDao {
    @Query(
        "SELECT * FROM enrichment_suggestion_cache " +
            "WHERE catalogId = :catalogId AND itemIdentity = :itemIdentity " +
            "AND targetAttributeKey = :targetAttributeKey AND knownAttributesFingerprint = :knownAttributesFingerprint " +
            "LIMIT 1",
    )
    suspend fun get(
        catalogId: String,
        itemIdentity: String,
        targetAttributeKey: String,
        knownAttributesFingerprint: String,
    ): EnrichmentSuggestionCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: EnrichmentSuggestionCacheEntity)

    @Query("DELETE FROM enrichment_suggestion_cache")
    suspend fun clear()
}
