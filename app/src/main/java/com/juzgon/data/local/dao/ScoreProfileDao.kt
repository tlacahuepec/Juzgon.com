package com.juzgon.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.juzgon.data.local.entity.ScoreProfileAttributeEntity
import com.juzgon.data.local.entity.ScoreProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreProfileDao {
    @Query("SELECT * FROM score_profiles WHERE category_name = :categoryName ORDER BY name")
    fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfileEntity>>

    @Query("SELECT * FROM score_profiles WHERE id = :id")
    fun observeProfile(id: String): Flow<ScoreProfileEntity?>

    @Upsert
    suspend fun upsertProfile(profile: ScoreProfileEntity)

    @Query("DELETE FROM score_profile_attributes WHERE profile_id = :profileId")
    suspend fun deleteAttributesForProfile(profileId: String)

    @Upsert
    suspend fun upsertProfileAttributes(attributes: List<ScoreProfileAttributeEntity>)

    @Query("SELECT * FROM score_profile_attributes WHERE profile_id = :profileId ORDER BY position")
    fun observeAttributesForProfile(profileId: String): Flow<List<ScoreProfileAttributeEntity>>

    @Query(
        """
        SELECT spa.* FROM score_profile_attributes spa
        INNER JOIN score_profiles sp ON sp.id = spa.profile_id
        WHERE sp.category_name = :categoryName
        ORDER BY spa.position
        """,
    )
    fun observeAttributesForCategory(categoryName: String): Flow<List<ScoreProfileAttributeEntity>>

    @Query("DELETE FROM score_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Transaction
    suspend fun saveProfileWithAttributes(
        profile: ScoreProfileEntity,
        attributes: List<ScoreProfileAttributeEntity>,
    ) {
        upsertProfile(profile)
        deleteAttributesForProfile(profile.id)
        upsertProfileAttributes(attributes)
    }
}
