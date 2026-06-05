package com.juzgon.domain.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialNetworkCodecTest {
    @Test
    fun `parse null returns empty list`() {
        assertEquals(emptyList<SocialNetworkEntry>(), SocialNetworkCodec.parse(null))
    }

    @Test
    fun `parse blank string returns empty list`() {
        assertEquals(emptyList<SocialNetworkEntry>(), SocialNetworkCodec.parse(""))
        assertEquals(emptyList<SocialNetworkEntry>(), SocialNetworkCodec.parse("   "))
    }

    @Test
    fun `parse empty JSON array returns empty list`() {
        assertEquals(emptyList<SocialNetworkEntry>(), SocialNetworkCodec.parse("[]"))
    }

    @Test
    fun `parse single entry returns single-element list`() {
        val json = """[{"platform":"INSTAGRAM","handle":"@testuser"}]"""
        val result = SocialNetworkCodec.parse(json)
        assertEquals(1, result.size)
        assertEquals(SocialPlatform.INSTAGRAM, result[0].platform)
        assertEquals("@testuser", result[0].handle)
    }

    @Test
    fun `parse multiple entries returns list in order`() {
        val json = """[{"platform":"INSTAGRAM","handle":"@user1"},{"platform":"TIKTOK","handle":"@user2"}]"""
        val result = SocialNetworkCodec.parse(json)
        assertEquals(2, result.size)
        assertEquals(SocialPlatform.INSTAGRAM, result[0].platform)
        assertEquals("@user1", result[0].handle)
        assertEquals(SocialPlatform.TIKTOK, result[1].platform)
        assertEquals("@user2", result[1].handle)
    }

    @Test
    fun `parse unknown platform key is skipped`() {
        val json = """[{"platform":"INSTAGRAM","handle":"@user1"},{"platform":"UNKNOWN_PLATFORM","handle":"@skip"}]"""
        val result = SocialNetworkCodec.parse(json)
        assertEquals(1, result.size)
        assertEquals(SocialPlatform.INSTAGRAM, result[0].platform)
    }

    @Test
    fun `parse malformed JSON returns empty list`() {
        assertEquals(emptyList<SocialNetworkEntry>(), SocialNetworkCodec.parse("not json"))
        assertEquals(emptyList<SocialNetworkEntry>(), SocialNetworkCodec.parse("{invalid"))
    }

    @Test
    fun `encode empty list returns empty JSON array`() {
        assertEquals("[]", SocialNetworkCodec.encode(emptyList()))
    }

    @Test
    fun `encode single entry produces valid JSON`() {
        val entries = listOf(SocialNetworkEntry(SocialPlatform.INSTAGRAM, "@testuser"))
        val json = SocialNetworkCodec.encode(entries)
        val parsed = SocialNetworkCodec.parse(json)
        assertEquals(entries, parsed)
    }

    @Test
    fun `encode round-trips multiple entries`() {
        val entries =
            listOf(
                SocialNetworkEntry(SocialPlatform.INSTAGRAM, "@user1"),
                SocialNetworkEntry(SocialPlatform.TIKTOK, "@user2"),
                SocialNetworkEntry(SocialPlatform.YOUTUBE, "channelname"),
            )
        val json = SocialNetworkCodec.encode(entries)
        val parsed = SocialNetworkCodec.parse(json)
        assertEquals(entries, parsed)
    }

    @Test
    fun `encode preserves handle exactly as provided`() {
        val entries = listOf(SocialNetworkEntry(SocialPlatform.X, "handle_with_underscore"))
        val json = SocialNetworkCodec.encode(entries)
        val parsed = SocialNetworkCodec.parse(json)
        assertEquals("handle_with_underscore", parsed[0].handle)
    }
}

class SocialPlatformUrlTest {
    @Test
    fun `Instagram URL strips @ prefix from handle`() {
        val entry = SocialNetworkEntry(SocialPlatform.INSTAGRAM, "@testuser")
        assertEquals("https://instagram.com/testuser", entry.profileUrl)
    }

    @Test
    fun `Instagram URL works without @ prefix`() {
        val entry = SocialNetworkEntry(SocialPlatform.INSTAGRAM, "testuser")
        assertEquals("https://instagram.com/testuser", entry.profileUrl)
    }

    @Test
    fun `Facebook URL construction`() {
        val entry = SocialNetworkEntry(SocialPlatform.FACEBOOK, "john.doe")
        assertEquals("https://facebook.com/john.doe", entry.profileUrl)
    }

    @Test
    fun `TikTok URL adds @ in path`() {
        val entry = SocialNetworkEntry(SocialPlatform.TIKTOK, "@tiktoker")
        assertEquals("https://tiktok.com/@tiktoker", entry.profileUrl)
    }

    @Test
    fun `TikTok URL adds @ when handle has no prefix`() {
        val entry = SocialNetworkEntry(SocialPlatform.TIKTOK, "tiktoker")
        assertEquals("https://tiktok.com/@tiktoker", entry.profileUrl)
    }

    @Test
    fun `OnlyFans URL construction`() {
        val entry = SocialNetworkEntry(SocialPlatform.ONLYFANS, "creator")
        assertEquals("https://onlyfans.com/creator", entry.profileUrl)
    }

    @Test
    fun `X URL construction`() {
        val entry = SocialNetworkEntry(SocialPlatform.X, "@xuser")
        assertEquals("https://x.com/xuser", entry.profileUrl)
    }

    @Test
    fun `YouTube URL construction`() {
        val entry = SocialNetworkEntry(SocialPlatform.YOUTUBE, "@channelname")
        assertEquals("https://youtube.com/@channelname", entry.profileUrl)
    }

    @Test
    fun `YouTube URL adds @ when handle has no prefix`() {
        val entry = SocialNetworkEntry(SocialPlatform.YOUTUBE, "channelname")
        assertEquals("https://youtube.com/@channelname", entry.profileUrl)
    }

    @Test
    fun `Threads URL construction`() {
        val entry = SocialNetworkEntry(SocialPlatform.THREADS, "@threaduser")
        assertEquals("https://threads.net/@threaduser", entry.profileUrl)
    }

    @Test
    fun `Threads URL adds @ when handle has no prefix`() {
        val entry = SocialNetworkEntry(SocialPlatform.THREADS, "threaduser")
        assertEquals("https://threads.net/@threaduser", entry.profileUrl)
    }

    @Test
    fun `all platforms have non-empty displayName`() {
        SocialPlatform.entries.forEach { platform ->
            assertTrue(
                "Platform ${platform.name} has empty displayName",
                platform.displayName.isNotBlank(),
            )
        }
    }
}
