# Juzgón UI & Navigation Prototype Specification

## Overview
This directory contains the interactive browser prototype and design reference assets created to overhaul Juzgón's visual presentation, information architecture, and navigation order.

It strictly preserves the **luminous dark theme design tokens** while establishing visual breathing room, eliminating layout cramping, and separating primary app destinations cleanly.

---

## High-Fidelity Design References

| Screen | Visual Asset | Key Design Principles |
|---|---|---|
| **Discover (Home)** | `juzgon_dashboard_redesign.jpg` | Minimalist header with title only; search input; Spotlight Hero card with glowing avatar and mini radar; collection stats; 2-column catalog previews; floating bottom nav. |
| **Category Detail** | `juzgon_category_items_redesign.jpg` | Sticky scoring lens selector; 2-column item grid; rank badges (#1, #2); tier pill badges; dedicated `+ Add New Persona` card. |
| **Item Detail (Radar)** | `juzgon_item_detail_redesign.jpg` | Centered avatar with animated neon glow ring; S-Tier glass score pill; 6-axis interactive radar polygon with concentric web circles; key metadata card. |
| **Item Detail (Bars)** | `juzgon_item_bars_redesign.jpg` | Sorted attribute ranking rows; pill-shaped gradient progress bars (purple ➔ cyan); fractional score readouts. |

---

## Interactive Web Prototype (`prototype.html`)

A zero-dependency, self-contained HTML5/CSS3/ES6 interactive application is located at [`prototype.html`](./prototype.html).

### How to Run:
- **Direct File**: Open `prototype.html` directly in any desktop or mobile browser.
- **Local Server**:
  ```powershell
  python -m http.server 8085 --directory docs/design/prototype
  ```
  Then visit: `http://localhost:8085/prototype.html`

### Key Interactive Flows to Test:
1. **Discover Screen**:
   - Clean top bar (app title only; all system/backup actions cleanly consolidated into Settings).
   - Tap Spotlight Hero card ➔ Navigates directly to the driver's Item Detail screen.
2. **Catalogs Screen**:
   - Filter category types (People, Films, Products).
   - Tap any catalog card ➔ Enters Category Detail.
3. **Category Detail Screen**:
   - Tap the **`➕ Add New Persona`** card or the top bar `➕` / FAB ➔ Opens the interactive Add Persona bottom sheet.
   - Test **`✨ Auto-fill with Gemini AI`** to populate attributes.
   - Adjust attribute score sliders and click **`Save Persona`** to see the new item added to the live grid with real-time rank updates.
   - Switch active scoring lenses (*Overall*, *Qualifying Pace*, *Race Craft*).
4. **Item Detail Screen**:
   - Toggle between **[Radar View]** (interactive Canvas radar drawing) and **[Score Bars]** (gradient progress bars).
   - Tap `←` back button to return to the catalog.
5. **Settings Screen**:
   - Full Database & Backup section (Export/Import JSON backup).
   - Gemini AI API Key configuration.
   - App architecture and build metadata.
