package com.juzgon.di

import android.content.Context
import androidx.room.Room
import com.juzgon.data.di.DataModule
import com.juzgon.data.di.EnrichmentModule
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.repository.RoomAttributeRankSnapshotRepository
import com.juzgon.data.repository.RoomCategoryRepository
import com.juzgon.data.repository.RoomRatedItemRepository
import com.juzgon.data.repository.RoomScoreProfileRepository
import com.juzgon.domain.AppClock
import com.juzgon.domain.BirthDateAgeCalculator
import com.juzgon.domain.BuildMetadata
import com.juzgon.domain.BuildMetadataProvider
import com.juzgon.domain.DateScoreCalculator
import com.juzgon.domain.backup.BackupService
import com.juzgon.domain.backup.BackupValidationResult
import com.juzgon.domain.backup.BackupValidator
import com.juzgon.domain.enrichment.AttributeEnrichmentProvider
import com.juzgon.domain.enrichment.AttributeEnrichmentRequest
import com.juzgon.domain.enrichment.AttributeEnrichmentResult
import com.juzgon.domain.enrichment.EnrichmentCacheKey
import com.juzgon.domain.enrichment.EnrichmentCachedResult
import com.juzgon.domain.enrichment.EnrichmentEventLogger
import com.juzgon.domain.enrichment.EnrichmentFailureCode
import com.juzgon.domain.enrichment.EnrichmentStatus
import com.juzgon.domain.enrichment.EnrichmentSuggestionCacheRepository
import com.juzgon.domain.enrichment.SecureApiKeyStore
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import com.juzgon.feature.item.ItemDetailDateProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.LocalDate
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class, EnrichmentModule::class])
object TestDataModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): JuzgonDatabase =
        Room
            .inMemoryDatabaseBuilder(context, JuzgonDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    @Singleton
    fun provideAppClock(): AppClock = AppClock { LocalDate.of(2026, 6, 3) }

    @Provides
    @Singleton
    fun provideBuildMetadataProvider(): BuildMetadataProvider =
        BuildMetadataProvider {
            BuildMetadata(
                versionName = "1.0.0-test",
                versionCode = 1,
                channel = "test",
                gitSha = "test",
                buildTimestamp = "0",
            )
        }

    @Provides
    @Singleton
    fun provideDateScoreCalculator(clock: AppClock): DateScoreCalculator = DateScoreCalculator(clock)

    @Provides
    @Singleton
    fun provideBirthDateAgeCalculator(clock: AppClock): BirthDateAgeCalculator = BirthDateAgeCalculator(clock)

    @Provides
    @Singleton
    fun provideItemDetailDateProcessor(
        birthDateAgeCalculator: BirthDateAgeCalculator,
        dateScoreCalculator: DateScoreCalculator,
    ): ItemDetailDateProcessor = ItemDetailDateProcessor(birthDateAgeCalculator, dateScoreCalculator)

    @Provides
    @Singleton
    fun provideCategoryRepository(database: JuzgonDatabase): CategoryRepository = RoomCategoryRepository(database)

    @Provides
    @Singleton
    fun provideRatedItemRepository(
        database: JuzgonDatabase,
        dateScoreCalculator: DateScoreCalculator,
    ): RatedItemRepository = RoomRatedItemRepository(database, dateScoreCalculator)

    @Provides
    @Singleton
    fun provideAttributeRankSnapshotRepository(database: JuzgonDatabase): AttributeRankSnapshotRepository =
        RoomAttributeRankSnapshotRepository(database)

    @Provides
    @Singleton
    fun provideScoreProfileRepository(db: JuzgonDatabase): ScoreProfileRepository = RoomScoreProfileRepository(db)

    @Provides
    @Singleton
    fun provideBackupValidator(): BackupValidator =
        object : BackupValidator {
            override fun validate(json: String): BackupValidationResult = BackupValidationResult()
        }

    @Provides
    @Singleton
    fun provideBackupService(): BackupService =
        object : BackupService {
            override suspend fun export(): String = "{}"

            override suspend fun import(json: String) {}
        }

    @Provides
    @Singleton
    fun provideSecureApiKeyStore(): SecureApiKeyStore =
        object : SecureApiKeyStore {
            override suspend fun saveGeminiApiKey(apiKey: String) {}

            override suspend fun getGeminiApiKey(): String? = null

            override suspend fun deleteGeminiApiKey() {}

            override suspend fun hasGeminiApiKey(): Boolean = false
        }

    @Provides
    @Singleton
    fun provideAttributeEnrichmentProvider(): AttributeEnrichmentProvider =
        object : AttributeEnrichmentProvider {
            override suspend fun enrichAttribute(request: AttributeEnrichmentRequest): AttributeEnrichmentResult =
                AttributeEnrichmentResult(status = EnrichmentStatus.ERROR, failureCode = EnrichmentFailureCode.MISSING_API_KEY)
        }

    @Provides
    @Singleton
    fun provideEnrichmentEventLogger(): EnrichmentEventLogger =
        object : EnrichmentEventLogger {
            override fun rejected(
                attributeKey: String,
                reason: String,
                originalStatus: String,
                confidence: String?,
            ) {}

            override fun accepted(
                attributeKey: String,
                itemId: String,
                suggestedValue: String,
            ) {}

            override fun dismissed(
                attributeKey: String,
                itemId: String,
            ) {}
        }

    @Provides
    @Singleton
    fun provideEnrichmentSuggestionCacheRepository(): EnrichmentSuggestionCacheRepository =
        object : EnrichmentSuggestionCacheRepository {
            override suspend fun get(key: EnrichmentCacheKey): EnrichmentCachedResult? = null

            override suspend fun put(result: EnrichmentCachedResult) {}

            override suspend fun clear() {}
        }
}
