# Troubleshooting

Use this directory for reusable, contributor-facing troubleshooting guides. Each guide should explain a recurring problem, how to recognize it, how to fix it, and how to verify the fix locally and in CI.

## Available guides

- [Build and CI](build-and-ci.md) - Gradle, Kotlin, KSP, ktlint, detekt, Spotless, and GitHub Actions issues.

## Adding a new troubleshooting guide

1. Copy [`TEMPLATE.md`](TEMPLATE.md) into a new, focused Markdown file.
2. Name the file by problem area, for example `android-studio-sync.md`, `dependency-upgrades.md`, or `release-signing.md`.
3. Keep the guide practical: include symptoms, root cause, fix steps, verification commands, and related files.
4. Link the new guide in the **Available guides** section above.
5. Prefer one focused guide per issue area instead of one large catch-all document.

## Documentation conventions

- Use relative links so docs render correctly on GitHub.
- Wrap file paths and Gradle tasks in backticks.
- Include exact commands in fenced `bash` code blocks.
- Document warnings separately from failures.
- Keep historical incident details brief and focus on repeatable recovery steps.

