**Starting work on #231 — Stop suppressing warnings (refactor instead of @Suppress)**

Branch: `feature/issue-231-stop-suppressing-warnings` (branched from latest `develop`)

Following the Engineering Constitution and repo process:
- Strict TDD: RED tests first for any area we touch.
- **Rule applied**: If we need to modify a class or method that lacks sufficient tests, we add the tests **first**.
- Small, focused, reviewable changes.
- All lints must stay clean.

### First slice
Targeting `ItemImageReferences.kt` (one of the files using `@file:Suppress("TooManyFunctions")`).

**Already done in this branch**:
- Added multiple new characterization tests in `ItemImageReferencesTest.kt` *before touching production code* (empty inputs, multiple references roundtrip, `buildImageReference` helper, etc.).
- This gives us a safety net for refactoring the image reference codec.

Will continue applying the same discipline to other files (Screens, ViewModels, DAOs, etc.).

In Progress. Will open PR with full RED→GREEN→REFACTOR evidence when the first meaningful slice is ready.