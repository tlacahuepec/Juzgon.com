package com.juzgon.domain.enrichment

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType

object EnrichmentSupportRules {
    fun isSupported(attribute: Attribute): Boolean =
        when (attribute.type) {
            AttributeType.DATE,
            AttributeType.DROPDOWN,
            AttributeType.NATIONALITY,
            AttributeType.SKIN_TYPE,
            -> true
            else -> false
        }
}
