package com.juzgon.domain.enrichment

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType

object EnrichmentSupportRules {
    private val supportedDateNames = setOf("birthdate", "birth date")

    fun isSupported(attribute: Attribute): Boolean =
        attribute.type == AttributeType.DATE &&
            attribute.displayName.trim().lowercase() in supportedDateNames
}
