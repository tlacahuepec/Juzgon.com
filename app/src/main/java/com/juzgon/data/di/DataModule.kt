package com.juzgon.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.juzgon.BuildConfig
import com.juzgon.data.backup.JsonBackupService
import com.juzgon.data.local.DatabaseMigrations
import com.juzgon.data.local.DebugSampleDataSeeder
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.RoomSampleDataStore
import com.juzgon.data.repository.RoomAttributeRankSnapshotRepository
import com.juzgon.data.repository.RoomCategoryRepository
import com.juzgon.data.repository.RoomRatedItemRepository
import com.juzgon.domain.backup.BackupService
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): JuzgonDatabase {
        lateinit var database: JuzgonDatabase
        val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        database =
            Room
                .databaseBuilder(context, JuzgonDatabase::class.java, "juzgon.db")
                .addMigrations(
                    DatabaseMigrations.MIGRATION_1_2,
                    DatabaseMigrations.MIGRATION_2_3,
                    DatabaseMigrations.MIGRATION_3_4,
                    DatabaseMigrations.MIGRATION_4_5,
                    DatabaseMigrations.MIGRATION_5_6,
                    DatabaseMigrations.MIGRATION_6_7,
                    DatabaseMigrations.MIGRATION_7_8,
                    DatabaseMigrations.MIGRATION_8_9,
                ).addCallback(
                    object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedScope.launch {
                                DebugSampleDataSeeder(
                                    isEnabled = BuildConfig.DEBUG,
                                    sampleDataStore = RoomSampleDataStore(database),
                                ).seed()
                            }
                        }
                    },
                ).build()
        return database
    }

    @Provides
    @Singleton
    fun provideBackupService(database: JuzgonDatabase): BackupService =
        JsonBackupService(
            categoryDao = database.categoryDao(),
            itemDao = database.itemDao(),
        )

    @Provides
    @Singleton
    fun provideCategoryRepository(database: JuzgonDatabase): CategoryRepository = RoomCategoryRepository(database)

    @Provides
    @Singleton
    fun provideRatedItemRepository(database: JuzgonDatabase): RatedItemRepository = RoomRatedItemRepository(database)

    @Provides
    @Singleton
    fun provideAttributeRankSnapshotRepository(database: JuzgonDatabase): AttributeRankSnapshotRepository =
        RoomAttributeRankSnapshotRepository(database)
}
