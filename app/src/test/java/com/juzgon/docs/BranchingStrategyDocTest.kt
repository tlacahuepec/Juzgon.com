package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BranchingStrategyDocTest {
    private val projectRoot = File(System.getProperty("user.dir")).parentFile

    @Test
    fun `branching strategy document exists`() {
        val file = File(projectRoot, "docs/branching-strategy.md")
        assertTrue("docs/branching-strategy.md must exist", file.exists())
    }

    @Test
    fun `document describes main branch as latest stable`() {
        val content = File(projectRoot, "docs/branching-strategy.md").readText()
        assertTrue(
            "Document must describe main as stable",
            content.contains("main", ignoreCase = true) &&
                content.contains("stable", ignoreCase = true),
        )
    }

    @Test
    fun `document describes develop branch as integration`() {
        val content = File(projectRoot, "docs/branching-strategy.md").readText()
        assertTrue(
            "Document must describe develop as integration branch",
            content.contains("develop", ignoreCase = true) &&
                content.contains("integration", ignoreCase = true),
        )
    }

    @Test
    fun `document describes feature branch naming`() {
        val content = File(projectRoot, "docs/branching-strategy.md").readText()
        assertTrue(
            "Document must describe feature/ branch naming",
            content.contains("feature/", ignoreCase = true),
        )
    }

    @Test
    fun `document describes hotfix flow`() {
        val content = File(projectRoot, "docs/branching-strategy.md").readText()
        assertTrue(
            "Document must describe hotfix flow",
            content.contains("hotfix", ignoreCase = true),
        )
    }

    @Test
    fun `document describes release promotion from develop to main`() {
        val content = File(projectRoot, "docs/branching-strategy.md").readText()
        assertTrue(
            "Document must describe promotion flow",
            content.contains("promotion", ignoreCase = true) ||
                content.contains("promote", ignoreCase = true) ||
                content.contains("release", ignoreCase = true),
        )
    }

    @Test
    fun `document describes PR workflow for merging`() {
        val content = File(projectRoot, "docs/branching-strategy.md").readText()
        assertTrue(
            "Document must reference pull requests",
            content.contains("pull request", ignoreCase = true) ||
                content.contains("PR", ignoreCase = false),
        )
    }
}
