package com.juzgon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.juzgon.data.local.dao.AttributeRankSnapshotDao
import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.DatabaseIntegrityDao
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.dao.ScoreProfileDao
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.AttributeRankSnapshotEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity
import com.juzgon.data.local.entity.ScoreProfileAttributeEntity
import com.juzgon.data.local.entity.ScoreProfileEntity

@Database(
    entities = [
        CategoryEntity::class,
        AttributeEntity::class,
        ItemEntity::class,
        RatingEntity::class,
        ItemValueEntity::class,
        AttributeRankSnapshotEntity::class,
        ScoreProfileEntity::class,
        ScoreProfileAttributeEntity::class,
    ],
    version = 15,
    exportSchema = true,
)
abstract class JuzgonDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    abstract fun itemDao(): ItemDao

    abstract fun attributeRankSnapshotDao(): AttributeRankSnapshotDao

    abstract fun scoreProfileDao(): ScoreProfileDao

    abstract fun databaseIntegrityDao(): DatabaseIntegrityDao
}
