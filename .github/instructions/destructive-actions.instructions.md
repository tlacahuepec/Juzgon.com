---
description: "Use when any task involves deleting files, overwriting existing content, bulk renaming, or removing code. Enforces safety review before destructive operations."
---

# Destructive Action Safety Rules

## What counts as destructive

- Deleting a file or directory
- Overwriting a file's full content (not a targeted edit)
- Bulk find-and-replace across many files
- Removing large blocks of existing code
- Dropping database tables or migrations

## Required behavior

1. **Stop and highlight** the destructive action before executing it.  
   Example: `⚠️ DESTRUCTIVE: This will delete app/src/main/java/Foo.kt — please confirm.`
2. **Wait for explicit user confirmation** unless the user's original request unambiguously calls for that specific deletion.
3. **Prefer targeted edits** (Edit tool, not full-file rewrite) whenever possible.
4. **Never use bulk operations** (e.g., `rm -rf`, overwriting multiple files at once) unless the user explicitly asks and confirms scope.

## Small-change principle

Each PR / task should touch the minimum set of files needed for the stated goal. Avoid incidental deletions or cleanups not related to the task.
