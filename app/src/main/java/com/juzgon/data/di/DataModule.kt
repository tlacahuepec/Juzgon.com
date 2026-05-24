package com.juzgon.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.juzgon.BuildConfig
import com.juzgon.data.backup.JsonBackupService
import com.juzgon.data.backup.JsonBackupValidator
import com.juzgon.data.local.DatabaseMigrations
import com.juzgon.data.local.DebugSampleDataSeeder
import com.juzgon.data.local.JuzgonDatabase
import com.juzgon.data.local.RoomSampleDataStore
import com.juzgon.data.repository.RoomAttributeRankSnapshotRepository
import com.juzgon.data.repository.RoomCategoryRepository
import com.juzgon.data.repository.RoomRatedItemRepository
import com.juzgon.data.repository.RoomScoreProfileRepository
import com.juzgon.domain.AppClock
import com.juzgon.domain.BuildMetadata
import com.juzgon.domain.BuildMetadataProvider
import com.juzgon.domain.DateScoreCalculator
import com.juzgon.domain.backup.BackupService
import com.juzgon.domain.backup.BackupValidator
import com.juzgon.domain.repository.AttributeRankSnapshotRepository
import com.juzgon.domain.repository.CategoryRepository
import com.juzgon.domain.repository.RatedItemRepository
import com.juzgon.domain.repository.ScoreProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
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
                    DatabaseMigrations.MIGRATION_9_10,
                    DatabaseMigrations.MIGRATION_10_11,
                    DatabaseMigrations.MIGRATION_11_12,
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
            scoreProfileDao = database.scoreProfileDao(),
        )

    @Provides
    @Singleton
    fun provideBackupValidator(): BackupValidator = JsonBackupValidator()

    @Provides
    @Singleton
    fun provideAppClock(): AppClock = AppClock { LocalDate.now() }

    @Provides
    @Singleton
    fun provideBuildMetadataProvider(): BuildMetadataProvider =
        BuildMetadataProvider {
            BuildMetadata(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                channel = BuildConfig.BUILD_CHANNEL,
                gitSha = BuildConfig.GIT_SHA,
                buildTimestamp = BuildConfig.BUILD_TIMESTAMP,
            )
        }

    @Provides
    @Singleton
    fun provideDateScoreCalculator(clock: AppClock): DateScoreCalculator = DateScoreCalculator(clock)

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
}
