package com.juzgon.data.di

import android.content.Context
import com.juzgon.data.schema.DefaultSchemaCompatibilityChecker
import com.juzgon.data.schema.SharedPrefsSchemaVersionProvider
import com.juzgon.domain.schema.SchemaCompatibilityChecker
import com.juzgon.domain.schema.StoredSchemaVersionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SchemaModule {
    @Provides
    @Singleton
    fun provideSchemaCompatibilityChecker(): SchemaCompatibilityChecker = DefaultSchemaCompatibilityChecker()

    @Provides
    @Singleton
    fun provideStoredSchemaVersionProvider(
        @ApplicationContext context: Context,
    ): StoredSchemaVersionProvider {
        val prefs = context.getSharedPreferences("juzgon_schema", Context.MODE_PRIVATE)
        return SharedPrefsSchemaVersionProvider(prefs)
    }
}
