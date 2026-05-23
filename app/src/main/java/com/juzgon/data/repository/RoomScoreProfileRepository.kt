package com.juzgon.data.repository

import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.mapper.toAttributeEntities
import com.juzgon.data.local.mapper.toDomain
import com.juzgon.data.local.mapper.toEntity
import com.juzgon.domain.ScoreProfile
import com.juzgon.domain.repository.ScoreProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class RoomScoreProfileRepository(
    database: JuzgonDatabase,
) : ScoreProfileRepository {
    private val scoreProfileDao = database.scoreProfileDao()

    override fun observeProfilesForCategory(categoryName: String): Flow<List<ScoreProfile>> =
        combine(
            scoreProfileDao.observeProfilesForCategory(categoryName),
            scoreProfileDao.observeAttributesForCategory(categoryName),
        ) { profiles, allAttributes ->
            val attributesByProfile = allAttributes.groupBy { it.profileId }
            profiles.map { profile ->
                val attributeIds = attributesByProfile[profile.id]?.map { it.attributeId }.orEmpty()
                profile.toDomain(attributeIds)
            }
        }.distinctUntilChanged()

    override fun observeProfile(id: String): Flow<ScoreProfile?> =
        combine(
            scoreProfileDao.observeProfile(id),
            scoreProfileDao.observeAttributesForProfile(id),
        ) { profile, attributes ->
            profile?.toDomain(attributes.map { it.attributeId })
        }.distinctUntilChanged()

    override suspend fun saveProfile(profile: ScoreProfile) {
        scoreProfileDao.saveProfileWithAttributes(
            profile = profile.toEntity(),
            attributes = profile.toAttributeEntities(),
        )
    }

    override suspend fun deleteProfile(id: String) {
        scoreProfileDao.deleteProfile(id)
    }
}
