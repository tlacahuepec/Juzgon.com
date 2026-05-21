package com.juzgon.domain.usecase

import com.juzgon.domain.Category
import java.util.Locale
import javax.inject.Inject

class ValidateCategoryUseCase
    @Inject
    constructor() {
        operator fun invoke(category: Category) {
            require(category.name.isNotBlank()) { "Category name cannot be blank" }
            require(category.attributes.isNotEmpty()) { "Category must contain at least one attribute" }

            val normalized = category.attributes.map { it.name.trim().lowercase(Locale.ROOT) }
            require(normalized.distinct().size == normalized.size) {
                "Category attributes must be unique (case-insensitive)"
            }
        }
    }
