package com.juzgon.domain.repository

import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    fun observeCategory(name: String): Flow<Category?>

    suspend fun saveCategory(category: Category)

    suspend fun renameCategory(
        originalName: String,
        category: Category,
    )

    suspend fun deleteCategory(name: String)
}

interface RatedItemRepository {
    fun observeRatedItems(): Flow<List<RatedItem>>

    fun observeRatedItem(id: String): Flow<RatedItem?>

    fun observeRankedItems(categoryName: String): Flow<List<RankedRatedItem>>

    suspend fun saveRatedItem(ratedItem: RatedItem)

    suspend fun deleteRatedItem(id: String)
}
