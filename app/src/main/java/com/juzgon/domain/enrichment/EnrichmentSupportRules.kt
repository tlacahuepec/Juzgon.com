package com.juzgon.domain.enrichment

import com.juzgon.domain.Attribute
import com.juzgon.domain.AttributeType

object EnrichmentSupportRules {
    fun isSupported(attribute: Attribute): Boolean =
        attribute.type == AttributeType.DATE &&
            attribute.displayName.equals("Birth Date", ignoreCase = true)
}
