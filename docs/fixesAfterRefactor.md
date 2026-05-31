fixes.md
----
JuzgonDatabase.kt

Best strict approach:

Keep one physical Room database (single file, single transaction boundary), but split API contracts into small interfaces.
Make feature code depend on those small interfaces, not on the full database.
Leave the database as the only implementation type that composes those interfaces.
Example structure idea:

CategoryDbAccess with only category-related DAO getters.
ItemDbAccess with item and rating DAO getters.
IntegrityDbAccess with integrity DAO getters.
EnrichmentDbAccess with enrichment cache DAO getter.
-----
Rating DAOS

Split Category DAO by responsibility
CategoryReadDao: observe/get category and attributes, item counts
CategoryWriteDao: upsert/delete categories and attributes
AttributeReferenceDao: rename attribute references across ratings/item_values/snapshots/profile_attributes
AttributeDependencyDao: count dependents before deletion
Split Item DAO by responsibility
ItemReadDao: get/observe item and ranked items
ItemWriteDao: upsert/delete items, ratings, values
Keep ItemPurgeDao as-is (already cohesive)
Keep snapshot DAO separate
AttributeRankSnapshotDao is already focused and small enough
Keep relation DTOs in a models file
Move CategoryItemCount, CategoryWithAttributes, ItemWithRatings, RankedItemWithRatings into a dedicated file like DaoModels.kt so DAO files stay contract-only

If you split too aggressively, constructor dependencies can explode in repositories. A good balance is 3-4 DAOs for category/item flows, not 10 tiny ones.