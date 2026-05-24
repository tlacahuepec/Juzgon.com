package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PrTemplateTest {
    private val projectRoot = File(System.getProperty("user.dir")).parentFile

    @Test
    fun `pr template exists`() {
        val file = File(projectRoot, ".github/pull_request_template.md")
        assertTrue(".github/pull_request_template.md must exist", file.exists())
    }

    @Test
    fun `pr template asks for linked issue`() {
        val content = File(projectRoot, ".github/pull_request_template.md").readText()
        assertTrue(
            "PR template must ask for linked issue",
            content.contains("Linked Issue") || content.contains("Closes #"),
        )
    }

    @Test
    fun `pr template contains target branch section`() {
        val content = File(projectRoot, ".github/pull_request_template.md").readText()
        assertTrue(
            "PR template must have target branch section",
            content.contains("Target Branch", ignoreCase = true),
        )
    }

    @Test
    fun `pr template references develop and main targets`() {
        val content = File(projectRoot, ".github/pull_request_template.md").readText()
        assertTrue(
            "PR template must reference develop and main",
            content.contains("develop") && content.contains("main"),
        )
    }

    @Test
    fun `pr template contains release or data impact section`() {
        val content = File(projectRoot, ".github/pull_request_template.md").readText()
        assertTrue(
            "PR template must have release/data impact section",
            content.contains("Release", ignoreCase = true) ||
                content.contains("Data Impact", ignoreCase = true) ||
                content.contains("schema", ignoreCase = true),
        )
    }

    @Test
    fun `pr template contains tdd evidence section`() {
        val content = File(projectRoot, ".github/pull_request_template.md").readText()
        assertTrue(
            "PR template must have TDD evidence section",
            content.contains("TDD", ignoreCase = true),
        )
    }
}
