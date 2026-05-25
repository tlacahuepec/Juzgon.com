package com.juzgon.data.local.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
@Suppress("TooManyFunctions")
interface DatabaseIntegrityDao {
    @Query("SELECT COUNT(*) FROM ratings WHERE attribute_id NOT IN (SELECT id FROM attributes)")
    suspend fun countOrphanRatings(): Int

    @Query(
        """
        SELECT item_id || ':' || attribute_id
        FROM ratings
        WHERE attribute_id NOT IN (SELECT id FROM attributes)
        ORDER BY item_id, attribute_id
        LIMIT :limit
        """,
    )
    suspend fun sampleOrphanRatings(limit: Int): List<String>

    @Query(
        """
        SELECT COUNT(*)
        FROM item_values
        WHERE attribute_id NOT IN (SELECT id FROM attributes)
          AND deleted_at IS NULL
        """,
    )
    suspend fun countOrphanActiveItemValues(): Int

    @Query(
        """
        SELECT item_id || ':' || attribute_id
        FROM item_values
        WHERE attribute_id NOT IN (SELECT id FROM attributes)
          AND deleted_at IS NULL
        ORDER BY item_id, attribute_id
        LIMIT :limit
        """,
    )
    suspend fun sampleOrphanActiveItemValues(limit: Int): List<String>

    @Query(
        """
        SELECT COUNT(*)
        FROM item_values
        WHERE attribute_id NOT IN (SELECT id FROM attributes)
          AND deleted_at IS NOT NULL
        """,
    )
    suspend fun countOrphanSoftDeletedItemValues(): Int

    @Query(
        """
        SELECT item_id || ':' || attribute_id
        FROM item_values
        WHERE attribute_id NOT IN (SELECT id FROM attributes)
          AND deleted_at IS NOT NULL
        ORDER BY item_id, attribute_id
        LIMIT :limit
        """,
    )
    suspend fun sampleOrphanSoftDeletedItemValues(limit: Int): List<String>

    @Query(
        """
        SELECT COUNT(*)
        FROM score_profiles sp
        LEFT JOIN score_profile_attributes spa ON spa.profile_id = sp.id
        WHERE spa.profile_id IS NULL
        """,
    )
    suspend fun countScoreProfilesWithoutAttributes(): Int

    @Query(
        """
        SELECT sp.id
        FROM score_profiles sp
        LEFT JOIN score_profile_attributes spa ON spa.profile_id = sp.id
        WHERE spa.profile_id IS NULL
        ORDER BY sp.id
        LIMIT :limit
        """,
    )
    suspend fun sampleScoreProfilesWithoutAttributes(limit: Int): List<String>

    @Query(
        """
        SELECT COUNT(*)
        FROM score_profile_attributes
        WHERE attribute_id NOT IN (SELECT id FROM attributes)
        """,
    )
    suspend fun countScoreProfileAttributesMissingAttributes(): Int

    @Query(
        """
        SELECT profile_id || ':' || attribute_id
        FROM score_profile_attributes
        WHERE attribute_id NOT IN (SELECT id FROM attributes)
        ORDER BY profile_id, attribute_id
        LIMIT :limit
        """,
    )
    suspend fun sampleScoreProfileAttributesMissingAttributes(limit: Int): List<String>

    @Query(
        """
        SELECT COUNT(*)
        FROM (
            SELECT c.name
            FROM categories c
            INNER JOIN attributes a ON a.category_name = c.name
            WHERE a.type = 'NUMBER'
               OR (a.type = 'DATE' AND a.scoring_direction IS NOT NULL)
            GROUP BY c.name
            HAVING COALESCE(SUM(a.weight), 0) = 0
        )
        """,
    )
    suspend fun countCategoriesWithZeroRankableWeight(): Int

    @Query(
        """
        SELECT c.name
        FROM categories c
        INNER JOIN attributes a ON a.category_name = c.name
        WHERE a.type = 'NUMBER'
           OR (a.type = 'DATE' AND a.scoring_direction IS NOT NULL)
        GROUP BY c.name
        HAVING COALESCE(SUM(a.weight), 0) = 0
        ORDER BY c.name
        LIMIT :limit
        """,
    )
    suspend fun sampleCategoriesWithZeroRankableWeight(limit: Int): List<String>
}
