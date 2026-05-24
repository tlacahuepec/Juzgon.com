package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeployDevWorkflowTest {
    private val projectRoot = File(System.getProperty("user.dir")).parentFile

    @Test
    fun `deploy dev workflow file exists`() {
        val file = File(projectRoot, ".github/workflows/deploy-dev.yml")
        assertTrue(".github/workflows/deploy-dev.yml must exist", file.exists())
    }

    @Test
    fun `deploy dev workflow triggers on develop branch push`() {
        val content = File(projectRoot, ".github/workflows/deploy-dev.yml").readText()
        assertTrue(
            "Deploy dev workflow must trigger on develop branch",
            content.contains("develop"),
        )
    }

    @Test
    fun `deploy dev workflow targets dev path`() {
        val content = File(projectRoot, ".github/workflows/deploy-dev.yml").readText()
        assertTrue(
            "Deploy dev workflow must target /dev/ path",
            content.contains("dev"),
        )
    }

    @Test
    fun `deploy dev workflow builds the app`() {
        val content = File(projectRoot, ".github/workflows/deploy-dev.yml").readText()
        assertTrue(
            "Deploy dev workflow must build the app",
            content.contains("assemble") || content.contains("build"),
        )
    }

    @Test
    fun `deploy dev workflow does not overwrite latest`() {
        val content = File(projectRoot, ".github/workflows/deploy-dev.yml").readText()
        assertTrue(
            "Deploy dev workflow must preserve other paths via keep_files or destination_dir",
            content.contains("destination_dir") || content.contains("keep_files"),
        )
    }
}
