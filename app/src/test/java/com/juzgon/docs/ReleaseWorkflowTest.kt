package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseWorkflowTest {
    private val projectRoot = File(System.getProperty("user.dir")).parentFile

    @Test
    fun `release workflow file exists`() {
        val file = File(projectRoot, ".github/workflows/release-pages.yml")
        assertTrue(".github/workflows/release-pages.yml must exist", file.exists())
    }

    @Test
    fun `release workflow triggers on version tags`() {
        val content = File(projectRoot, ".github/workflows/release-pages.yml").readText()
        assertTrue(
            "Release workflow must trigger on version tags",
            content.contains("v*.*.*") || content.contains("v[0-9]"),
        )
    }

    @Test
    fun `release workflow builds the app`() {
        val content = File(projectRoot, ".github/workflows/release-pages.yml").readText()
        assertTrue(
            "Release workflow must build the app",
            content.contains("assembleRelease") || content.contains("bundleRelease"),
        )
    }

    @Test
    fun `release workflow creates a github release`() {
        val content = File(projectRoot, ".github/workflows/release-pages.yml").readText()
        assertTrue(
            "Release workflow must create a GitHub release",
            content.contains("gh-release") ||
                content.contains("action-gh-release") ||
                content.contains("create-release") ||
                content.contains("Release"),
        )
    }

    @Test
    fun `release workflow deploys to github pages`() {
        val content = File(projectRoot, ".github/workflows/release-pages.yml").readText()
        assertTrue(
            "Release workflow must deploy to GitHub Pages",
            content.contains("gh-pages") || content.contains("pages"),
        )
    }

    @Test
    fun `release workflow validates tag format`() {
        val content = File(projectRoot, ".github/workflows/release-pages.yml").readText()
        assertTrue(
            "Release workflow must validate tag format",
            content.contains("tag") && content.contains("valid"),
        )
    }
}
