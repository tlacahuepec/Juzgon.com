package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchitectureDocTest {
    private val projectRoot: File =
        File(checkNotNull(System.getProperty("user.dir"))).let { dir ->
            if (dir.name == "app") checkNotNull(dir.parentFile) else dir
        }

    @Test
    fun `architecture document exists`() {
        val file = File(projectRoot, "docs/architecture.md")
        assertTrue("docs/architecture.md must exist", file.exists())
    }

    @Test
    fun `architecture document covers clean architecture and boundaries`() {
        val content = File(projectRoot, "docs/architecture.md").readText()
        assertTrue(
            "Document must reference Clean Architecture and boundary checks",
            content.contains("Clean Architecture", ignoreCase = true) &&
                content.contains("checkDependencyBoundaries"),
        )
    }

    @Test
    fun `architecture document covers domain layer`() {
        val content = File(projectRoot, "docs/architecture.md").readText()
        assertTrue(
            "Document must reference domain entities and models",
            content.contains("Domain Layer", ignoreCase = true) &&
                content.contains("RatingModels"),
        )
    }

    @Test
    fun `architecture document covers persistence and room database`() {
        val content = File(projectRoot, "docs/architecture.md").readText()
        assertTrue(
            "Document must reference JuzgonDatabase and Room",
            content.contains("JuzgonDatabase") &&
                content.contains("Room", ignoreCase = true),
        )
    }

    @Test
    fun `architecture document covers ai attribute enrichment`() {
        val content = File(projectRoot, "docs/architecture.md").readText()
        assertTrue(
            "Document must reference Gemini attribute enrichment",
            content.contains("Gemini", ignoreCase = true) &&
                content.contains("Enrichment", ignoreCase = true),
        )
    }

    @Test
    fun `architecture document covers visual design and components`() {
        val content = File(projectRoot, "docs/architecture.md").readText()
        assertTrue(
            "Document must reference visual tokens and custom components",
            content.contains("Visual Tokens", ignoreCase = true) &&
                content.contains("JuzgonRadarChart"),
        )
    }

    @Test
    fun `readme links to architecture document`() {
        val readme = File(projectRoot, "README.md").readText()
        assertTrue(
            "README.md must link to docs/architecture.md",
            readme.contains("docs/architecture.md"),
        )
    }
}
