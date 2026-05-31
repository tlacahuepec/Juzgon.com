# Issue 234 UI Beautification Spike

## Context

Issue #234 asks for a design spike around the four new visual mockups in `mockups/`:

| Mockup | Permanent reference | Intended surface |
|--------|---------------------|------------------|
| Home | `mockups/home.jpg` | Home / discovery dashboard |
| Card view | `mockups/cardview.jpg` | Collection and ranked item cards |
| Diamond chart | `mockups/diamond.jpg` | Item detail diamond profile |
| Bars | `mockups/Bars.jpg` | Attribute score bars |

The images remain in `mockups/` for this spike. This document is the stable design reference and should be linked from follow-up implementation issues.

## RED Evidence

The spike tests are design-verification artifacts rather than executable Kotlin tests. They establish the current/proposed delta before any production UI rewrite.

| Required check | Evidence |
|----------------|----------|
| Current vs proposed visual differences | Documented in the four mockup sections below. |
| Usability and accessibility check | Documented in the accessibility section. |
| Component inventory | Documented in the Compose component inventory section. |
| Risk assessment | Documented in the risks section. |

## Visual Direction

### Palette

The mockups move Juzgon toward a dark, luminous, collection-oriented visual system:

| Role | Approximate colors | Notes |
|------|--------------------|-------|
| Base background | `#05040A`, `#100717`, `#1B0B25` | Nearly black surfaces with subtle purple undertones. |
| Primary glow | `#C026D3`, `#D946EF`, `#E879F9` | Magenta/pink glow around cards, avatars, charts, active navigation. |
| Secondary glow | `#7C3AED`, `#8B5CF6`, `#A855F7` | Violet gradients for selected tabs, chart fills, and hero accents. |
| Contrast accent | `#22D3EE`, `#2DD4BF` | Cyan/teal used sparingly for score pills and high-emphasis labels. |
| Rating accent | `#FBBF24`, `#FDBA74`, `#FB7185` | Stars and bar end stops; reserve for rating metadata. |
| Text | `#FFFFFF`, `#F5F3FF`, `#C4B5FD`, `#A1A1AA` | Strong white headings, muted secondary labels. |

The current theme already has dark purple foundations in `JuzgonDarkColorScheme`, but it is Material 3-token driven and restrained. The mockups add stronger gradients, larger imagery, more glow, and denser score metadata.

### Typography

- Proposed headings are bold, compact, and high-contrast, with app/screen names around the current Material title-large to headline-small range.
- Card titles use strong weight and short labels; metadata stays compact.
- Scores are large display anchors in hero and profile views.
- Current app typography uses default `MaterialTheme.typography`; follow-up work should introduce named semantic text roles before hand-tuning per screen.

### Spacing and Layout

- Mockups use dense vertical rhythm, strong first-screen hierarchy, and horizontally grouped navigation.
- Hero surfaces use wide image cards with overlaid score and chart metadata.
- Collection cards use fixed grid tracks and stable card heights.
- Profile charts center the object/image first, then surround it with score visualizations.
- Current Compose screens mostly use `LazyColumn`, `Column`, `Row`, `Scaffold`, `TopAppBar`, Material cards/surfaces, and 16 dp padding. The first implementation slice should keep those structures and replace surface treatments incrementally.

### Shape, Elevation, and Effects

- Mockups use rounded cards, circular avatar frames, pill tabs, translucent bottom navigation, and luminous outlines.
- Elevation is expressed through glow, blur-like shadows, translucent surfaces, and gradient borders rather than standard Material elevation alone.
- Compose does not provide gradient borders or blur effects as first-class Material components, so the reusable layer should start with tokens for shapes and gradients, then add small helper composables only where necessary.

### Iconography

- Mockups rely on simple outline icons: home, grid/collection, favorite, profile, search, back, notifications, and arrows.
- The current app already uses Material icons and explicit accessibility roles. Keep icon choices simple and label every icon-only action through existing semantics patterns.
- Emoji-like attribute icons in the mockups should not become the default system until they can be generated from category metadata or configured by users.

## Mockup Review

### Home

Current direction: `HomeScreen` is a category management hub with sort chips, category rows, import/export/about/settings actions, and a floating add button.

Proposed direction:

- Add a high-impact hero panel for the best or most recent rated item/category, but keep the existing category-management tasks discoverable.
- Add compact collection stats if they can be derived without new persistence.
- Use card imagery only when the rated item or category has an image; otherwise provide a polished placeholder.
- Avoid hard-coding "trending" or recommendation language until there is real ranking or recommendation logic.

First slice fit: high. Home is the most visible surface and can absorb new tokens, hero card treatment, and stats without data-model changes if the copy stays generic.

### Card View

Current direction: `CategoryDetailItemCard` displays item image, title, rank, overall score, score profile result, active sorted attribute, and optional movement indicators.

Proposed direction:

- Convert item cards into a reusable visual collection card with stable image/avatar area, title, tier/score row, favorite affordance placeholder only if favorites are implemented, and optional compact attribute rows.
- Preserve current sort, rank, and edit/detail click behaviors.
- Keep card text category-neutral. The mockups' sample labels are content examples, not product copy.

First slice fit: high, after tokens exist. It can reuse existing image and attribute models.

### Diamond Chart

Current direction: `ItemDetailScreen` already contains `DiamondChartSection` and `ItemAttributeDiamondChart`, drawing a radar chart from `DiamondChartPoint` values.

Proposed direction:

- Upgrade the chart surface with darker container, brighter polygon fill/stroke, labeled outer points, and an item image anchor above the chart.
- Keep the chart implementation data-driven and category-neutral.
- Consider a reusable `JuzgonRadarChart` after one screen proves the API.

First slice fit: medium-high. The chart is already present, but label density and screen real estate need mobile testing.

### Bars

Current direction: `RankedAttributeProgressCards` renders individual attribute cards with `LinearProgressIndicator`.

Proposed direction:

- Replace separated progress cards with a grouped attribute-score list for item detail.
- Introduce gradient progress bars and stronger right-aligned score labels.
- Keep 48 dp minimum touch targets for any interactive rows.
- Reserve decorative per-attribute icons for a later metadata-backed enhancement.

First slice fit: medium. Bars are visually quick, but gradient progress treatment should come after shared tokens to avoid one-off styling.

## Compose Component Inventory

| Surface | Current files | Major components and helpers |
|---------|---------------|------------------------------|
| Home | `app/src/main/java/com/juzgon/feature/home/HomeScreen.kt` | `HomeRoute`, `HomeScreen`, `HomeContent`, `HomeHeader`, `HomeSortControls`, `HomeCategoryContent`, `HomeEmptyState`, `CategoryRow` |
| Category detail / cards | `app/src/main/java/com/juzgon/feature/category/CategoryDetailScreen.kt` | `CategoryDetailRoute`, `CategoryDetailScreen`, `CategoryDetailItemList`, `ProfileSelector`, `InlineSortChips`, `CompactSortTrigger`, `CategoryDetailItemCard`, `CategoryDetailItemVisual`, `CategoryDetailItemOverlay`, `CategoryDetailItemTitle` |
| Item detail / diamond / bars | `app/src/main/java/com/juzgon/feature/item/ItemDetailScreen.kt` | `ItemDetailRoute`, `ItemDetailScreen`, `ItemDetailContent`, `OverallScoreSection`, `ProfileBreakdownSection`, `DiamondChartSection`, `ItemAttributeDiamondChart`, `PrimaryImageSection`, `RankedAttributeProgressCards`, `RankedAttributeCard`, `AttributeValuesSection` |
| Item form | `app/src/main/java/com/juzgon/feature/item/ItemFormScreen.kt` | `ItemFormRoute`, `ItemFormScreen`, `ItemFormContent`, `ItemScoreField`, `ItemAttributeValueField`, `ImageAttributeValueField`, `ImageAttributePreview` |
| Score profiles | `app/src/main/java/com/juzgon/feature/scoreprofile/ScoreProfileListScreen.kt`, `ScoreProfileFormScreen.kt` | `ScoreProfileListRoute`, `ScoreProfileListScreen`, `ProfileCard`, score profile form route/screen and attribute selectors |
| Navigation | `app/src/main/java/com/juzgon/navigation/JuzgonNavigation.kt` | Navigation host, routes, and app-level destinations |
| Theme | `app/src/main/java/com/juzgon/ui/theme/JuzgonTheme.kt` | `JuzgonLightColorScheme`, `JuzgonDarkColorScheme`, `JuzgonTheme`, `JuzgonThemeSelector` |

Reusable candidates:

- `JuzgonVisualTokens`: gradient, glow, shape, and spacing tokens that sit beside the current Material color schemes.
- `JuzgonHeroCard`: home-first visual card with image fallback, score text, and optional chart thumbnail slot.
- `JuzgonCollectionCard`: shared item/category card shell for category detail and future collection views.
- `JuzgonScorePill`: reusable tier/score label.
- `JuzgonGradientProgressBar`: determinate bar that preserves accessibility semantics from `LinearProgressIndicator`.
- `JuzgonRadarChart`: extracted only after the item-detail chart refresh proves the API.

## Accessibility Check

- Contrast: white text on the proposed near-black surfaces is safe. Purple/magenta labels on dark backgrounds need verification before implementation; use white text for primary actions and restrict neon colors to borders, fills, and icons.
- Touch targets: keep current `IconButton`, `Button`, `FilterChip`, and navigation controls at Material minimum sizes. Do not shrink mockup pill controls below 48 dp height for interactive targets.
- Motion/effects: glow and gradients should be static first. Add animation only after respecting reduced-motion expectations.
- Semantics: preserve existing content descriptions and roles in the touched screens. Visual-only score treatments must still expose score text to assistive tech.
- The mockups include content-specific labels and body-focused attribute examples. The implementation should stay category-neutral and use user-defined attribute names from the app data model.

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Mockup copy is not category-neutral | Could narrow the product tone and make generic rating categories feel incorrect | Treat mockup labels as placeholders; preserve user/category data copy. |
| Visual refresh becomes a full redesign | Large PR, hard review, likely regressions | Start with tokens plus one screen slice per PR. |
| Glow/gradient effects become one-off code | Inconsistent styling and harder accessibility checks | Introduce shared tokens/helpers before screen rewrites. |
| Hero/recommendation concepts imply missing data | Requires ranking, recommendations, or favorite state that may not exist | Use existing category/item/rank data only; defer recommendation/favorite features. |
| Diamond chart labels overcrowd small screens | Reduced readability and overlapping content | Verify with compact viewport screenshots before merging chart changes. |
| New visual surfaces hide existing actions | Users could lose import/export/settings/category management access | Preserve current actions and navigation in the first Home slice. |

No data-model or navigation changes are required for the first visual slice. Favorites, recommendations, notification badges, and configurable attribute icons would require separate product/data decisions.

## Phased Rollout Plan

1. Spike and issue planning: keep this document as the design reference, create follow-up issues, and close #234 through a docs PR.
2. Visual foundation PR: add shared visual tokens and small preview/test coverage without changing major screen layout.
3. Home quick-win PR: refresh Home hierarchy with a hero/category summary treatment while preserving current actions.
4. Card quick-win PR: extract and apply a reusable collection card to category detail.
5. Profile visualization PR: refresh diamond chart and attribute bars in item detail.
6. Broader redesigns: bottom navigation, recommendation concepts, favorites, and richer imagery only after the first slices are stable.

## First Slice Recommendation

Start with visual tokens plus Home. Home has the highest first-run impact, currently concentrates many app-level actions, and can use existing category/rated-item data without schema or navigation changes.

## Follow-Up Issue Drafts

These implementation issues were created from this spike:

| Priority | Title | Scope |
|----------|-------|-------|
| 1 | [#244 Add reusable visual design tokens for Juzgon refresh](https://github.com/tlacahuepec/Juzgon.com/issues/244) | Define additive dark/glow/gradient/shape tokens and focused tests around token selection. |
| 2 | [#245 Refresh Home visual hierarchy using issue #234 mockup direction](https://github.com/tlacahuepec/Juzgon.com/issues/245) | Add a category/item hero surface, compact stats, and refreshed spacing while preserving existing actions. |
| 3 | [#246 Extract reusable collection card for category detail items](https://github.com/tlacahuepec/Juzgon.com/issues/246) | Centralize card imagery, score pill, rank/attribute metadata, and accessibility semantics. |
| 4 | [#247 Refresh item detail diamond chart and attribute score bars](https://github.com/tlacahuepec/Juzgon.com/issues/247) | Improve radar chart and grouped score bars using shared visual tokens. |

## Definition of Done Status

- Clear documented visual direction extracted from the mockups: done in this document.
- Prioritized list of follow-up issues ready to be worked: documented above.
- Decision on first slice to implement: visual tokens plus Home.
- Findings recorded in linked design doc: this file.
