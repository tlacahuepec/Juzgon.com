# Migration to UUID-based Data Architecture (Resolves #74)

## Overview
This pull request transitions the core entities (`Category`, `Attribute`, `Item`, `ItemValue`) from relying on name-based primary keys to using robust UUID-based unique identifiers (`id`). 

## Key Changes
- **Data Model Migration**: 
  - Updated `CategoryEntity`, `AttributeEntity`, `ItemEntity` to hold UUID fields for relational mapping.
  - Room database handles migration cleanly using destructive fallback.
- **Data Layer Updates**: 
  - `CategoryRepository` now receives a stable `originalId` parameter instead of string-based name checks.
- **UI & ViewModels**: 
  - `HomeViewModel`, `CategoryFormViewModel`, `CategoryDetailViewModel`, `ItemFormViewModel`, and `ItemDetailViewModel` are completely unlinked from tracking `categoryName` as the source of truth, and updated to seamlessly hold `categoryId` state.
  - UI labels updated to extract names from `Category`/`Item` domain structures correctly.
- **Navigation Update**:
  - `JuzgonNavigation.kt` correctly maps route strings utilizing the `categoryId` & `itemId` identifiers securely instead of URI-encoded name components.

## Impact
This structurally fixes the foundational data architecture so that:
1. **Mutability**: Category and Item names can be edited or renamed freely without impacting cascading foreign keys or destroying item associations.
2. **Robustness**: Identical names are handled natively without causing unique-constraint collisions on item or category creation.

This concludes Phase 1 & 2 of the roadmap.
