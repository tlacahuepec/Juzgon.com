package com.juzgon.feature.item

import androidx.compose.runtime.mutableStateListOf
import com.juzgon.domain.social.SocialNetworkCodec
import com.juzgon.domain.social.SocialNetworkEntry
import com.juzgon.domain.social.SocialPlatform

internal class SocialNetworkEditorState(
    initialJson: String,
) {
    private val _entries = mutableStateListOf<SocialNetworkEntry>()
    val entries: List<SocialNetworkEntry> get() = _entries

    init {
        _entries.addAll(SocialNetworkCodec.parse(initialJson))
    }

    fun addEntry(
        platform: SocialPlatform,
        handle: String,
        attributeId: String,
        onValueChange: (String, String) -> Unit,
    ) {
        _entries.add(SocialNetworkEntry(platform, handle))
        onValueChange(attributeId, SocialNetworkCodec.encode(_entries))
    }

    fun removeEntry(
        index: Int,
        attributeId: String,
        onValueChange: (String, String) -> Unit,
    ) {
        if (index in _entries.indices) {
            _entries.removeAt(index)
            onValueChange(attributeId, SocialNetworkCodec.encode(_entries))
        }
    }

    fun updateFromExternalValue(json: String) {
        _entries.clear()
        _entries.addAll(SocialNetworkCodec.parse(json))
    }
}
