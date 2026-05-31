# Issue 244 Visual Token Contract

Issue #244 adds an additive visual-token layer for the Juzgon UI refresh. The token layer is intentionally separate from `JuzgonLightColorScheme` and `JuzgonDarkColorScheme` so existing Material fallback behavior stays stable while later UI slices can opt into the refresh direction.

## Stable Tokens

Stable tokens are reusable primitives intended for Home, card, diamond chart, and bar-refresh follow-ups:

| Token group | Purpose |
|-------------|---------|
| `JuzgonVisualPalette` | Dark luminous surfaces, glow accents, rating accents, and high-contrast text colors. |
| `JuzgonVisualGradients` | Shared gradient stops for hero surfaces, glow borders, and score bars. |
| `JuzgonVisualShapes` | Shared card, pill, and avatar corner radii. |
| `JuzgonVisualSpacing` | Shared spacing steps for dense visual-refresh layouts. |

The stable token source is `JuzgonVisualTokenSelector.refreshTokens()`. `JuzgonTheme` provides those tokens through `JuzgonVisualTheme.tokens` without requiring dynamic color.

## Experimental Helpers

No screen-specific helper composables ship in this issue. Follow-up issues should introduce helpers only after a concrete surface proves the API:

- `JuzgonHeroCard` belongs with the Home refresh.
- `JuzgonCollectionCard` belongs with the category detail card extraction.
- `JuzgonScorePill`, `JuzgonGradientProgressBar`, and `JuzgonRadarChart` should be added with the item detail visualization work that first consumes them.

## Compatibility

The token layer does not change `JuzgonThemeSelector.fallbackColorScheme`, dynamic color selection, or the Material color schemes. Existing light/dark behavior remains the source of truth for app-wide Material components until individual refresh slices opt into the new visual tokens.
