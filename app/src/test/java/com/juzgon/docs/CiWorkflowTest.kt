package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CiWorkflowTest {
    private val projectRoot = File(System.getProperty("user.dir")).parentFile

    @Test
    fun `ci workflow file exists`() {
        val file = File(projectRoot, ".github/workflows/android-ci.yml")
        assertTrue(".github/workflows/android-ci.yml must exist", file.exists())
    }

    @Test
    fun `ci workflow triggers on push to develop branch`() {
        val content = File(projectRoot, ".github/workflows/android-ci.yml").readText()
        assertTrue(
            "CI workflow must trigger on push to develop",
            content.contains("develop"),
        )
    }

    @Test
    fun `ci workflow triggers on pull requests`() {
        val content = File(projectRoot, ".github/workflows/android-ci.yml").readText()
        assertTrue(
            "CI workflow must trigger on pull_request",
            content.contains("pull_request"),
        )
    }

    @Test
    fun `ci workflow runs ktlint check`() {
        val content = File(projectRoot, ".github/workflows/android-ci.yml").readText()
        assertTrue(
            "CI workflow must run ktlintCheck",
            content.contains("ktlintCheck"),
        )
    }

    @Test
    fun `ci workflow runs detekt`() {
        val content = File(projectRoot, ".github/workflows/android-ci.yml").readText()
        assertTrue(
            "CI workflow must run detekt",
            content.contains("detekt"),
        )
    }

    @Test
    fun `ci workflow runs unit tests`() {
        val content = File(projectRoot, ".github/workflows/android-ci.yml").readText()
        assertTrue(
            "CI workflow must run testDebugUnitTest",
            content.contains("testDebugUnitTest"),
        )
    }

    @Test
    fun `ci workflow runs spotless check`() {
        val content = File(projectRoot, ".github/workflows/android-ci.yml").readText()
        assertTrue(
            "CI workflow must run spotlessCheck",
            content.contains("spotlessCheck"),
        )
    }
}
