package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseProcessDocTest {
    private val projectRoot = File(System.getProperty("user.dir")).parentFile

    @Test
    fun `release process document exists`() {
        val file = File(projectRoot, "docs/release-process.md")
        assertTrue("docs/release-process.md must exist", file.exists())
    }

    @Test
    fun `document includes tag command example`() {
        val content = File(projectRoot, "docs/release-process.md").readText()
        assertTrue(
            "Document must include git tag command",
            content.contains("git tag"),
        )
    }

    @Test
    fun `document includes versioned github pages urls`() {
        val content = File(projectRoot, "docs/release-process.md").readText()
        assertTrue(
            "Document must reference versioned GitHub Pages paths",
            content.contains("/latest/") || content.contains("/v"),
        )
    }

    @Test
    fun `document includes verification steps`() {
        val content = File(projectRoot, "docs/release-process.md").readText()
        assertTrue(
            "Document must include verification steps",
            content.contains("verif", ignoreCase = true),
        )
    }

    @Test
    fun `document references app version and data schema version`() {
        val content = File(projectRoot, "docs/release-process.md").readText()
        assertTrue(
            "Document must reference versioning concepts",
            content.contains("version", ignoreCase = true) &&
                content.contains("schema", ignoreCase = true),
        )
    }

    @Test
    fun `document includes rollback guidance`() {
        val content = File(projectRoot, "docs/release-process.md").readText()
        assertTrue(
            "Document must include rollback guidance",
            content.contains("rollback", ignoreCase = true) ||
                content.contains("revert", ignoreCase = true),
        )
    }
}
