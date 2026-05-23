package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BranchProtectionDocTest {
    private val projectRoot = File(System.getProperty("user.dir")).parentFile

    @Test
    fun `branch protection document exists`() {
        val file = File(projectRoot, "docs/branch-protection.md")
        assertTrue("docs/branch-protection.md must exist", file.exists())
    }

    @Test
    fun `document describes main branch protection rules`() {
        val content = File(projectRoot, "docs/branch-protection.md").readText()
        assertTrue(
            "Document must describe main branch rules",
            content.contains("main", ignoreCase = true) &&
                content.contains("require", ignoreCase = true),
        )
    }

    @Test
    fun `document describes develop branch protection rules`() {
        val content = File(projectRoot, "docs/branch-protection.md").readText()
        assertTrue(
            "Document must describe develop branch rules",
            content.contains("develop", ignoreCase = true),
        )
    }

    @Test
    fun `document requires pull requests before merge`() {
        val content = File(projectRoot, "docs/branch-protection.md").readText()
        assertTrue(
            "Document must require pull requests",
            content.contains("pull request", ignoreCase = true),
        )
    }

    @Test
    fun `document requires status checks to pass`() {
        val content = File(projectRoot, "docs/branch-protection.md").readText()
        assertTrue(
            "Document must reference status checks",
            content.contains("status check", ignoreCase = true) ||
                content.contains("CI", ignoreCase = false),
        )
    }

    @Test
    fun `document references validation workflow`() {
        val content = File(projectRoot, "docs/branch-protection.md").readText()
        assertTrue(
            "Document must reference validation workflow",
            content.contains("android-ci", ignoreCase = true) ||
                content.contains("validation", ignoreCase = true) ||
                content.contains("workflow", ignoreCase = true),
        )
    }
}
