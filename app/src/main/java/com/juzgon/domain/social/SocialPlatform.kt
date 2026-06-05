package com.juzgon.domain.social

enum class SocialPlatform(
    val displayName: String,
    val urlTemplate: String,
) {
    INSTAGRAM("Instagram", "https://instagram.com/{handle}"),
    FACEBOOK("Facebook", "https://facebook.com/{handle}"),
    TIKTOK("TikTok", "https://tiktok.com/@{handle}"),
    ONLYFANS("OnlyFans", "https://onlyfans.com/{handle}"),
    X("X (Twitter)", "https://x.com/{handle}"),
    YOUTUBE("YouTube", "https://youtube.com/@{handle}"),
    THREADS("Threads", "https://threads.net/@{handle}"),
    ;

    fun buildUrl(handle: String): String {
        val cleaned = handle.removePrefix("@")
        return urlTemplate.replace("{handle}", cleaned)
    }
}
