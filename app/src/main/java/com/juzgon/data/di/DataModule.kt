package com.juzgon.data.di

import android.content.Context
import androidx.room.Room
import com.juzgon.data.local.DatabaseMigrations
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.repository.RoomCategoryRepository
import com.juzgon.data.repository.RoomRatedItemRepository
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): JuzgonDatabase =
        Room
            .databaseBuilder(context, JuzgonDatabase::class.java, "juzgon.db")
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideCategoryRepository(database: JuzgonDatabase): CategoryRepository = RoomCategoryRepository(database)

    @Provides
    @Singleton
    fun provideRatedItemRepository(database: JuzgonDatabase): RatedItemRepository = RoomRatedItemRepository(database)
}
