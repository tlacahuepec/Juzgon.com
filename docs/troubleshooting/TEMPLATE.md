# <Problem Area>

## Summary

Briefly describe the problem area and when contributors should use this guide.

## Affected commands or workflows

List the local commands, Gradle tasks, GitHub Actions jobs, or IDE workflows affected by the issue.

```bash
# Example
./gradlew <task-name>
```

## Symptoms

Document the error messages or behavior contributors are likely to see.

```text
Paste the most relevant error excerpt here.
```

## Root cause

Explain why the issue happens. Keep this short, factual, and tied to project files or tool versions when possible.

## Fix

Describe the expected fix. Reference exact files and settings.

1. Update `<file>`.
2. Change `<setting>`.
3. Re-run the verification commands.

## Verification

Include commands that prove the fix works locally and, when relevant, match GitHub Actions.

```bash
./gradlew <verification-task>
```

Expected result:

```text
BUILD SUCCESSFUL
```

## Known warnings

List warnings that may still appear after the fix and whether they require action.

## Related files

- `<path/to/file>`
- `<path/to/workflow.yml>`

## References

- Add links to relevant project docs, tool docs, GitHub Actions workflows, or upstream issues.

