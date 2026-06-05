package com.juzgon.domain.social

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object SocialNetworkCodec {
    private const val KEY_PLATFORM = "platform"
    private const val KEY_HANDLE = "handle"

    fun parse(raw: String?): List<SocialNetworkEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = Json.parseToJsonElement(raw).jsonArray
            array.mapNotNull { element ->
                val obj = element.jsonObject
                val platformName =
                    obj[KEY_PLATFORM]?.jsonPrimitive?.content ?: return@mapNotNull null
                val platform =
                    SocialPlatform.entries.firstOrNull { it.name == platformName }
                        ?: return@mapNotNull null
                val handle = obj[KEY_HANDLE]?.jsonPrimitive?.content ?: return@mapNotNull null
                SocialNetworkEntry(platform, handle)
            }
        }.getOrDefault(emptyList())
    }

    fun encode(entries: List<SocialNetworkEntry>): String {
        val array =
            buildJsonArray {
                entries.forEach { entry ->
                    add(
                        buildJsonObject {
                            put(KEY_PLATFORM, entry.platform.name)
                            put(KEY_HANDLE, entry.handle)
                        },
                    )
                }
            }
        return array.toString()
    }
}
