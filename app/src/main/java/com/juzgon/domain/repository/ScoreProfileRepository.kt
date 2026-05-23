package com.juzgon.domain.repository

import com.juzgon.domain.ScoreProfile
import kotlinx.coroutines.flow.Flow

interface ScoreProfileRepository {
    fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfile>>

    fun observeProfile(id: String): Flow<ScoreProfile?>

    suspend fun saveProfile(profile: ScoreProfile)

    suspend fun deleteProfile(id: String)
}
