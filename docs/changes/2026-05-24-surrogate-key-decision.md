# 2026-05-24 — Surrogate key strategy for categories and attributes

## Context

The current schema uses `categories.name` as the primary key and string-based attribute IDs (e.g. `"Speed"`) as the primary key in the `attributes` table. Foreign keys in `ratings`, `item_values`, `attribute_rank_snapshots`, `score_profiles`, and `score_profile_attributes` all reference these string keys.

This design makes category and attribute rename operations complex: a rename must cascade through every referencing table inside a transaction. The backup JSON format also embeds these string IDs.

This document evaluates whether introducing surrogate (UUID) keys would simplify integrity, and whether to implement now or defer.

## Options Considered

### Option A: Current string-key model (status quo)

| Aspect | Assessment |
|--------|-----------|
| Rename complexity | High — requires multi-table cascading updates |
| Migration risk | None — no schema change |
| Backup compatibility | Full — current format uses string keys directly |
| Query simplicity | High — human-readable keys, no joins needed to resolve names |
| Data loss risk | None |

**Pros:** No migration, no backup format versioning, simple debugging.
**Cons:** Rename operations touch 7+ tables; rename bugs can cause orphaned rows.

### Option B: Surrogate category ID only

Add a `category_id UUID` column to `categories`; migrate all FK references from `categoryName TEXT` to `category_id UUID`.

| Aspect | Assessment |
|--------|-----------|
| Rename complexity | Low for categories, unchanged for attributes |
| Migration risk | Medium — 4 tables reference `categoryName` |
| Backup compatibility | Requires format versioning (v5) with ID mapping |
| Data loss risk | Medium — FK migration must backfill correctly |

**Pros:** Category renames become a single row update.
**Cons:** Attributes still use string keys; backup format must handle both old and new schemas; partial solution.

### Option C: Surrogate category ID + surrogate attribute ID

Add UUIDs to both `categories` and `attributes`. All FK columns become UUID-based.

| Aspect | Assessment |
|--------|-----------|
| Rename complexity | Minimal — renames are single-row name updates |
| Migration risk | High — every table with attribute/category FK must migrate |
| Backup compatibility | Major format change; requires dual-read compatibility layer |
| Data loss risk | High — 7 tables need correct backfill in a single migration |

**Pros:** Cleanest long-term model; renames are trivial.
**Cons:** Highest risk migration; touches every DAO, repository, and mapper; backup format needs v5 with full ID mapping; extensive test coverage required.

### Option D: Hybrid compatibility layer

Add surrogate UUID columns as nullable additions. Keep existing string FK columns. New code writes both; reads prefer UUID when present, fall back to string. Remove string columns after validation period.

| Aspect | Assessment |
|--------|-----------|
| Rename complexity | Low after full adoption |
| Migration risk | Low per step — additive columns first, then gradual cutover |
| Backup compatibility | Can maintain backward-compatible export during transition |
| Data loss risk | Low — old columns remain until validated |

**Pros:** Incremental; rollback is trivial (drop new columns); backup format can evolve gradually.
**Cons:** Temporary code complexity (dual-read logic); longer timeline; more intermediate states to test.

## Affected Tables

| Table | Current key/FK | Would change under B | Would change under C/D |
|-------|---------------|---------------------|----------------------|
| `categories` | PK: `name TEXT` | Add `id UUID`, keep `name` | Same |
| `attributes` | PK: `id TEXT`, FK: `categoryName TEXT` | FK → `category_id UUID` | PK → `uuid`, FK → `category_id UUID` |
| `ratings` | FK: `attributeId TEXT` | No change | FK → attribute UUID |
| `item_values` | FK: `attributeId TEXT` | No change | FK → attribute UUID |
| `attribute_rank_snapshots` | FK: `attributeId TEXT` | No change | FK → attribute UUID |
| `score_profiles` | FK: `categoryName TEXT` | FK → `category_id UUID` | Same |
| `score_profile_attributes` | FK: `attributeId TEXT` | No change | FK → attribute UUID |
| Backup JSON | Uses string IDs | Needs v5 format with category ID mapping | Needs v5 format with full ID mapping |

## Data Preservation Requirements

Any future implementation MUST follow these steps in order:

1. **Backup before migration** — export full backup in current format before any schema change.
2. **Additive columns first** — add new UUID columns as nullable; never drop existing columns in the same migration.
3. **Backfill plan** — populate new UUID columns from existing data in a dedicated migration step.
4. **Dual-read or validation phase** — run both old and new FK paths; compare results; log discrepancies.
5. **Rollback strategy** — if validation fails, drop the new columns (data preserved in originals).
6. **No destructive cleanup until validation passes** — old string columns remain until dual-read confirms zero discrepancies across at least one release cycle.

## Recommendation

**Defer implementation. Keep the current string-key model (Option A).**

Rationale:

1. The recent work on `BackupAttributeIdNormalizer` (#175) and transactional import (#171) already mitigates the highest-risk rename scenarios by validating references before writes.
2. The rename operation complexity is manageable with the current transaction-based approach and the integrity guardrails being added in #173.
3. A surrogate key migration touches every table, DAO, repository, mapper, and the backup format — this is a multi-sprint effort with high data-loss risk if rushed.
4. The hybrid approach (Option D) is the safest path IF migration becomes necessary, but the current pain level does not justify the complexity.

**Trigger to revisit:** If category/attribute rename frequency increases significantly, or if a new feature requires stable cross-device entity references (sync, sharing), re-evaluate Option D.

## Verification

This is a decision document. No code changes required.

```bash
# Confirm document exists and is well-formed
cat docs/changes/2026-05-24-surrogate-key-decision.md
```

## Risks and known limitations

- This decision defers complexity rather than eliminating it. A future surrogate migration will still be needed if stable entity references become a product requirement.
- The backup format (currently v4) is coupled to string keys. Adding surrogate keys later will require a v5 format with backward-compatible import.

## Follow-ups

- [ ] Re-evaluate if rename frequency or sync requirements change
- [ ] If approved later, implement via Option D (hybrid) with phased rollout
- [ ] Backup format v5 design (only needed if surrogate keys are implemented)

## References

- Parent epic: #167 (Database Hardening)
- Related: #175 (Backup attribute ID normalization)
- Related: #171 (Transactional backup import)
- Related: #173 (Category rename integrity guardrails)
