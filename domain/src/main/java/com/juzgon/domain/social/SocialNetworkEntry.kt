package com.juzgon.domain.social

data class SocialNetworkEntry(
    val platform: SocialPlatform,
    val handle: String,
) {
    val profileUrl: String get() = platform.buildUrl(handle)
}
