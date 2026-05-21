package com.juzgon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.juzgon.data.local.dao.CategoryDao
import com.juzgon.data.local.dao.ItemDao
import com.juzgon.data.local.entity.AttributeEntity
import com.juzgon.data.local.entity.CategoryEntity
import com.juzgon.data.local.entity.ItemEntity
import com.juzgon.data.local.entity.ItemValueEntity
import com.juzgon.data.local.entity.RatingEntity

@Database(
    entities = [
        CategoryEntity::class,
        AttributeEntity::class,
        ItemEntity::class,
        RatingEntity::class,
        ItemValueEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class JuzgonDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    abstract fun itemDao(): ItemDao
}
