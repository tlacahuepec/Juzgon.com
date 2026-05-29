@file:Suppress("MaxLineLength")

package com.juzgon.data.di

import android.content.Context
import com.juzgon.data.enrichment.GeminiAttributeEnrichmentProvider
import com.juzgon.data.enrichment.RoomEnrichmentSuggestionCacheRepository
import com.juzgon.data.enrichment.TimberEnrichmentEventLogger
import com.juzgon.data.local.dao.EnrichmentSuggestionCacheDao
import com.juzgon.data.security.EncryptedApiKeyStore
import com.juzgon.domain.enrichment.AttributeEnrichmentProvider
import com.juzgon.domain.enrichment.EnrichmentEventLogger
import com.juzgon.domain.enrichment.EnrichmentSuggestionCacheRepository
import com.juzgon.domain.enrichment.SecureApiKeyStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EnrichmentModule {
    @Provides
    @Singleton
    fun provideSecureApiKeyStore(
        @ApplicationContext context: Context,
    ): SecureApiKeyStore = EncryptedApiKeyStore(context)

    @Provides
    @Singleton
    fun provideAttributeEnrichmentProvider(provider: GeminiAttributeEnrichmentProvider): AttributeEnrichmentProvider = provider

    @Provides
    @Singleton
    fun provideEnrichmentEventLogger(impl: TimberEnrichmentEventLogger): EnrichmentEventLogger = impl

    @Provides
    @Singleton
    fun provideEnrichmentSuggestionCacheRepository(dao: EnrichmentSuggestionCacheDao): EnrichmentSuggestionCacheRepository =
        RoomEnrichmentSuggestionCacheRepository(dao)
}
