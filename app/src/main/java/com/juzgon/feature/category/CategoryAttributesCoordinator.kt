@file:Suppress("TooManyFunctions")

package com.juzgon.feature.category

import com.juzgon.domain.AttributeType
import com.juzgon.domain.ScoringDirection
import com.juzgon.domain.repository.RatedItemRepository
import kotlinx.coroutines.flow.first

/**
 * Extracted coordinator responsible for all attribute-related form logic.
 * This removes a large number of methods and complexity from CategoryFormViewModel,
 * directly addressing the @Suppress("TooManyFunctions").
 */
class CategoryAttributesCoordinator(
    private val ratedItemRepository: RatedItemRepository,
) {
    private var nextAttributeKey = 1L
    private var dirtyAttributeKeys = emptySet<Long>()

    var attributes: List<CategoryAttributeInput> = emptyList()
        private set

    fun initializeForNewCategory() {
        nextAttributeKey = 1L
        dirtyAttributeKeys = emptySet()
        attributes = emptyList()
    }

    suspend fun initializeForEdit(
        categoryName: String,
        existingAttributes: List<CategoryAttributeInput>,
    ) {
        nextAttributeKey = existingAttributes.size.toLong().coerceAtLeast(1)
        attributes = existingAttributes

        val dirtyAttributeIds =
            ratedItemRepository
                .observeRankedItems(categoryName)
                .first()
                .flatMap { ranked ->
                    ranked.item.scores.map { it.attribute.id } + ranked.item.values.map { it.attribute.id }
                }.toSet()

        dirtyAttributeKeys =
            existingAttributes
                .filter { it.sourceId in dirtyAttributeIds }
                .map { it.key }
                .toSet()
    }

    fun addAttribute() {
        val newAttr = CategoryAttributeInput(key = nextAttributeKey++)
        attributes = attributes + newAttr
    }

    fun removeAttribute(key: Long): RemoveAttributeResult =
        if (key in dirtyAttributeKeys) {
            pendingDeleteKey = key
            RemoveAttributeResult.RequiresConfirmation(key)
        } else {
            doRemoveAttribute(key)
            RemoveAttributeResult.Removed
        }

    fun confirmAttributeDeletion(): Boolean {
        val key = pendingDeleteKey ?: return false
        dirtyAttributeKeys = dirtyAttributeKeys - key
        doRemoveAttribute(key)
        pendingDeleteKey = null
        return true
    }

    fun declineAttributeDeletion() {
        pendingDeleteKey = null
    }

    var pendingDeleteKey: Long? = null
        private set

    fun moveAttributeUp(key: Long) = moveAttribute(key, -1)

    fun moveAttributeDown(key: Long) = moveAttribute(key, 1)

    private fun moveAttribute(
        key: Long,
        offset: Int,
    ) {
        val fromIndex = attributes.indexOfFirst { it.key == key }
        val toIndex = fromIndex + offset
        if (fromIndex == -1 || toIndex !in attributes.indices) return

        val reordered = attributes.toMutableList()
        val attr = reordered.removeAt(fromIndex)
        reordered.add(toIndex, attr)
        attributes = reordered
    }

    // Attribute field updaters
    fun updateName(
        key: Long,
        name: String,
    ) = update(key) { it.copy(name = name) }

    fun updateWeight(
        key: Long,
        weightText: String,
    ) = update(key) { it.copy(weightText = weightText) }

    fun updateRequired(
        key: Long,
        isRequired: Boolean,
    ) = update(key) { it.copy(isRequired = isRequired) }

    fun updateType(
        key: Long,
        type: AttributeType,
    ): TypeChangeResult =
        if (key in dirtyAttributeKeys) {
            pendingTypeChange = type
            pendingTypeChangeKey = key
            TypeChangeResult.RequiresConfirmation(key, type)
        } else {
            update(key) { it.withType(type) }
            TypeChangeResult.Applied
        }

    fun confirmTypeChange() {
        val key = pendingTypeChangeKey ?: return
        val type = pendingTypeChange ?: return
        update(key) { it.withType(type) }
        clearTypeChangeState()
    }

    fun declineTypeChange() {
        clearTypeChangeState()
    }

    var pendingTypeChange: AttributeType? = null
        private set
    var pendingTypeChangeKey: Long? = null
        private set

    fun updateDisplayInDiamond(
        key: Long,
        display: Boolean,
    ) {
        update(key) { attr ->
            if (attr.isRankable) attr.copy(displayInDiamond = display) else attr
        }
    }

    fun updateDiamondOrder(
        key: Long,
        orderText: String,
    ) {
        update(key) { attr ->
            if (attr.isRankable) attr.copy(diamondOrderText = orderText) else attr
        }
    }

    fun updateScoringDirection(
        key: Long,
        direction: ScoringDirection?,
    ) {
        update(key) { attr ->
            if (attr.type == AttributeType.DATE) attr.copy(scoringDirection = direction) else attr
        }
    }

    private fun update(
        key: Long,
        transform: (CategoryAttributeInput) -> CategoryAttributeInput,
    ) {
        attributes = attributes.map { if (it.key == key) transform(it) else it }
    }

    private fun doRemoveAttribute(key: Long) {
        attributes = attributes.filterNot { it.key == key }
    }

    private fun clearTypeChangeState() {
        pendingTypeChange = null
        pendingTypeChangeKey = null
    }

    private fun CategoryAttributeInput.withType(type: AttributeType): CategoryAttributeInput {
        val newDirection = if (type == AttributeType.DATE) scoringDirection else null
        val rankable = type == AttributeType.NUMBER || (type == AttributeType.DATE && newDirection != null)
        return copy(
            type = type,
            displayInDiamond = rankable && displayInDiamond,
            diamondOrderText = if (rankable) diamondOrderText else "",
            scoringDirection = newDirection,
        )
    }

    // Result types for warning flows
    sealed interface RemoveAttributeResult {
        object Removed : RemoveAttributeResult

        data class RequiresConfirmation(
            val key: Long,
        ) : RemoveAttributeResult
    }

    sealed interface TypeChangeResult {
        object Applied : TypeChangeResult

        data class RequiresConfirmation(
            val key: Long,
            val newType: AttributeType,
        ) : TypeChangeResult
    }

    fun getCurrentAttributes(): List<CategoryAttributeInput> = attributes.toList()

    fun getDirtyKeys(): Set<Long> = dirtyAttributeKeys
}
