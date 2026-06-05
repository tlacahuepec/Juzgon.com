package com.juzgon.feature.item

import com.juzgon.domain.social.SocialNetworkCodec
import com.juzgon.domain.social.SocialPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialNetworkEditorStateTest {
    @Test
    fun `initial empty string produces empty entries`() {
        val state = SocialNetworkEditorState("")
        assertTrue(state.entries.isEmpty())
    }

    @Test
    fun `initial JSON produces parsed entries`() {
        val json = """[{"platform":"INSTAGRAM","handle":"@user1"}]"""
        val state = SocialNetworkEditorState(json)
        assertEquals(1, state.entries.size)
        assertEquals(SocialPlatform.INSTAGRAM, state.entries[0].platform)
        assertEquals("@user1", state.entries[0].handle)
    }

    @Test
    fun `addEntry appends and invokes callback with encoded JSON`() {
        val state = SocialNetworkEditorState("")
        var callbackValue = ""
        state.addEntry(SocialPlatform.INSTAGRAM, "@user1", "attr1") { _, value ->
            callbackValue = value
        }

        assertEquals(1, state.entries.size)
        assertEquals(SocialPlatform.INSTAGRAM, state.entries[0].platform)
        val parsed = SocialNetworkCodec.parse(callbackValue)
        assertEquals(1, parsed.size)
        assertEquals("@user1", parsed[0].handle)
    }

    @Test
    fun `addEntry appends to existing entries`() {
        val json = """[{"platform":"INSTAGRAM","handle":"@user1"}]"""
        val state = SocialNetworkEditorState(json)
        var callbackValue = ""
        state.addEntry(SocialPlatform.TIKTOK, "@user2", "attr1") { _, value ->
            callbackValue = value
        }

        assertEquals(2, state.entries.size)
        val parsed = SocialNetworkCodec.parse(callbackValue)
        assertEquals(2, parsed.size)
        assertEquals(SocialPlatform.TIKTOK, parsed[1].platform)
    }

    @Test
    fun `addEntry allows duplicate platform with different handle`() {
        val json = """[{"platform":"INSTAGRAM","handle":"@user1"}]"""
        val state = SocialNetworkEditorState(json)
        state.addEntry(SocialPlatform.INSTAGRAM, "@user2", "attr1") { _, _ -> }

        assertEquals(2, state.entries.size)
    }

    @Test
    fun `removeEntry removes by index and invokes callback`() {
        val json =
            """[{"platform":"INSTAGRAM","handle":"@user1"},{"platform":"TIKTOK","handle":"@user2"}]"""
        val state = SocialNetworkEditorState(json)
        var callbackValue = ""
        state.removeEntry(0, "attr1") { _, value -> callbackValue = value }

        assertEquals(1, state.entries.size)
        assertEquals(SocialPlatform.TIKTOK, state.entries[0].platform)
        val parsed = SocialNetworkCodec.parse(callbackValue)
        assertEquals(1, parsed.size)
    }

    @Test
    fun `removeEntry last entry produces empty JSON`() {
        val json = """[{"platform":"INSTAGRAM","handle":"@user1"}]"""
        val state = SocialNetworkEditorState(json)
        var callbackValue = ""
        state.removeEntry(0, "attr1") { _, value -> callbackValue = value }

        assertTrue(state.entries.isEmpty())
        assertEquals("[]", callbackValue)
    }

    @Test
    fun `updateFromExternalValue replaces entries`() {
        val state = SocialNetworkEditorState("")
        val json =
            """[{"platform":"YOUTUBE","handle":"@channel"},{"platform":"X","handle":"@handle"}]"""
        state.updateFromExternalValue(json)

        assertEquals(2, state.entries.size)
        assertEquals(SocialPlatform.YOUTUBE, state.entries[0].platform)
        assertEquals(SocialPlatform.X, state.entries[1].platform)
    }
}
