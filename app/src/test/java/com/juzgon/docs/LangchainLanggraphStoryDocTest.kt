package com.juzgon.docs

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class LangchainLanggraphStoryDocTest {
    private val storyPath: Path =
        listOf(
            Path.of("docs", "design", "issue-langchain-langgraph-evaluation.md"),
            Path.of("..", "docs", "design", "issue-langchain-langgraph-evaluation.md"),
        ).firstOrNull(Files::exists)
            ?: Path.of("docs", "design", "issue-langchain-langgraph-evaluation.md")

    @Test
    fun `story documents learning as an explicit evaluation benefit`() {
        val story = storyPath.readRequiredText()

        assertTrue(story.contains("learn LangChain/LangGraph"))
        assertTrue(story.contains("learning value"))
    }

    @Test
    fun `story defines decision acceptance criteria and spike deliverables`() {
        val story = storyPath.readRequiredText()

        listOf(
            "## Acceptance Criteria",
            "## Tests to write first (RED)",
            "## Spike Deliverables",
            "## Decision Criteria",
            "## Recommended First Experiment",
        ).forEach { requiredSection ->
            assertTrue("Missing section: $requiredSection", story.contains(requiredSection))
        }
    }

    private fun Path.readRequiredText(): String {
        assertTrue("Expected story document to exist at $this", Files.exists(this))
        return readText()
    }
}
