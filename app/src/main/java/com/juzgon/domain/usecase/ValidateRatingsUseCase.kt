package com.juzgon.domain.usecase

import javax.inject.Inject

class ValidateRatingsUseCase
    @Inject
    constructor() {
        data class AttributeBounds(
            val attributeId: String,
            val minScore: Int = MIN_SCORE,
            val maxScore: Int = MAX_SCORE,
        ) {
            init {
                require(attributeId.isNotBlank()) { "Attribute id cannot be blank" }
                require(minScore in MIN_SCORE..MAX_SCORE) { "minScore must be between 1 and 10" }
                require(maxScore in MIN_SCORE..MAX_SCORE) { "maxScore must be between 1 and 10" }
                require(minScore <= maxScore) { "minScore must be less than or equal to maxScore" }
            }
        }

        operator fun invoke(
            scoresByAttributeId: Map<String, Int>,
            requiredAttributeBounds: List<AttributeBounds>,
        ) {
            val requiredIds = requiredAttributeBounds.map { it.attributeId }
            require(requiredIds.distinct().size == requiredIds.size) {
                "Required attribute ids must be unique"
            }

            requiredAttributeBounds.forEach { bound ->
                val score =
                    scoresByAttributeId[bound.attributeId]
                        ?: throw IllegalArgumentException("Missing score for required attribute '${bound.attributeId}'")

                require(score in bound.minScore..bound.maxScore) {
                    "Score for attribute '${bound.attributeId}' must be between ${bound.minScore} and ${bound.maxScore}"
                }
            }
        }

        private companion object {
            const val MIN_SCORE = 1
            const val MAX_SCORE = 10
        }
    }
