package com.juzgon.domain.usecase

import com.juzgon.domain.Attribute
import com.juzgon.domain.ScoreProfile
import javax.inject.Inject

class ValidateScoreProfileUseCase
    @Inject
    constructor() {
        operator fun invoke(
            profile: ScoreProfile,
            existingProfiles: List<ScoreProfile>,
            categoryAttributes: List<Attribute>,
        ) {
            val otherProfiles = existingProfiles.filter { it.id != profile.id }
            require(otherProfiles.none { it.name == profile.name }) {
                "A score profile named '${profile.name}' already exists in this category"
            }

            val attributeMap = categoryAttributes.associateBy { it.id }
            profile.includedAttributeIds.forEach { attrId ->
                val attribute = attributeMap[attrId]
                require(attribute != null) {
                    "Attribute '$attrId' does not exist in this category"
                }
                require(attribute.isRankable) {
                    "Attribute '$attrId' is not rankable"
                }
            }
        }
    }
