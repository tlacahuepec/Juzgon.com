package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VersionIndexTest {
    private val projectRoot = File(System.getProperty("user.dir")).parentFile

    @Test
    fun `version index generation script exists`() {
        val file = File(projectRoot, ".github/scripts/generate-versions-index.sh")
        assertTrue(
            ".github/scripts/generate-versions-index.sh must exist",
            file.exists(),
        )
    }

    @Test
    fun `script generates versions json structure`() {
        val content = File(projectRoot, ".github/scripts/generate-versions-index.sh").readText()
        assertTrue(
            "Script must produce versions.json",
            content.contains("versions.json"),
        )
    }

    @Test
    fun `script includes latest field`() {
        val content = File(projectRoot, ".github/scripts/generate-versions-index.sh").readText()
        assertTrue(
            "Script must include latest field in output",
            content.contains("latest"),
        )
    }

    @Test
    fun `script includes version entries with url`() {
        val content = File(projectRoot, ".github/scripts/generate-versions-index.sh").readText()
        assertTrue(
            "Script must produce entries with url field",
            content.contains("url"),
        )
    }

    @Test
    fun `script includes channel field`() {
        val content = File(projectRoot, ".github/scripts/generate-versions-index.sh").readText()
        assertTrue(
            "Script must include channel in version entries",
            content.contains("channel"),
        )
    }

    @Test
    fun `release workflow references version index script`() {
        val content = File(projectRoot, ".github/workflows/release-pages.yml").readText()
        assertTrue(
            "Release workflow must reference version index generation",
            content.contains("versions") || content.contains("generate-versions"),
        )
    }
}
