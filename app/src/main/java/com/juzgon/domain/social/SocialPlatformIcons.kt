package com.juzgon.domain.social

import com.juzgon.R

object SocialPlatformIcons {
    fun iconRes(platform: SocialPlatform): Int =
        when (platform) {
            SocialPlatform.INSTAGRAM -> R.drawable.ic_social_instagram
            SocialPlatform.FACEBOOK -> R.drawable.ic_social_facebook
            SocialPlatform.TIKTOK -> R.drawable.ic_social_tiktok
            SocialPlatform.ONLYFANS -> R.drawable.ic_social_onlyfans
            SocialPlatform.X -> R.drawable.ic_social_x
            SocialPlatform.YOUTUBE -> R.drawable.ic_social_youtube
            SocialPlatform.THREADS -> R.drawable.ic_social_threads
        }
}
