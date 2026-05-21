package com.juzgon.domain.repository

import com.juzgon.domain.Category
import com.juzgon.domain.RankedRatedItem
import com.juzgon.domain.RatedItem
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    fun observeCategory(id: String): Flow<Category?>

    suspend fun saveCategory(category: Category)

    suspend fun renameCategory(
        originalId: String,
        category: Category,
    )

    suspend fun deleteCategory(id: String)
}

interface RatedItemRepository {
    fun observeRatedItems(): Flow<List<RatedItem>>

    fun observeRatedItem(id: String): Flow<RatedItem?>

    fun observeRankedItems(categoryId: String): Flow<List<RankedRatedItem>>

    suspend fun saveRatedItem(ratedItem: RatedItem)

    suspend fun deleteRatedItem(id: String)
}
