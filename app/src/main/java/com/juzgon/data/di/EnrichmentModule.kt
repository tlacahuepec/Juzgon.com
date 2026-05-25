package com.juzgon.data.di

import android.content.Context
import com.juzgon.data.security.EncryptedApiKeyStore
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
}
