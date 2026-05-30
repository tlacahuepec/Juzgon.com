package com.juzgon.data.local

import com.juzgon.data.local.dao.CategoryIntegrityDao
import com.juzgon.data.local.dao.ItemValueIntegrityDao
import com.juzgon.data.local.dao.RatingIntegrityDao
import com.juzgon.data.local.dao.ScoreProfileIntegrityDao

private const val DEFAULT_SAMPLE_LIMIT = 5

data class DatabaseIntegrityFinding(
    val count: Int,
    val sampleIds: List<String>,
)

data class DatabaseIntegrityReport(
    val orphanRatings: DatabaseIntegrityFinding,
    val orphanActiveItemValues: DatabaseIntegrityFinding,
    val orphanSoftDeletedItemValues: DatabaseIntegrityFinding,
    val scoreProfilesWithoutAttributes: DatabaseIntegrityFinding,
    val scoreProfileAttributesMissingAttributes: DatabaseIntegrityFinding,
    val categoriesWithZeroRankableWeight: DatabaseIntegrityFinding,
)

/**
 * Updated to use the split integrity DAOs after the large DatabaseIntegrityDao split.
 */
class DatabaseIntegrityRepository(
    private val ratingIntegrityDao: RatingIntegrityDao,
    private val itemValueIntegrityDao: ItemValueIntegrityDao,
    private val scoreProfileIntegrityDao: ScoreProfileIntegrityDao,
    private val categoryIntegrityDao: CategoryIntegrityDao,
    private val sampleLimit: Int = DEFAULT_SAMPLE_LIMIT,
) {
    suspend fun diagnose(): DatabaseIntegrityReport =
        DatabaseIntegrityReport(
            orphanRatings =
                DatabaseIntegrityFinding(
                    count = ratingIntegrityDao.countOrphanRatings(),
                    sampleIds = ratingIntegrityDao.sampleOrphanRatings(sampleLimit),
                ),
            orphanActiveItemValues =
                DatabaseIntegrityFinding(
                    count = itemValueIntegrityDao.countOrphanActiveItemValues(),
                    sampleIds = itemValueIntegrityDao.sampleOrphanActiveItemValues(sampleLimit),
                ),
            orphanSoftDeletedItemValues =
                DatabaseIntegrityFinding(
                    count = itemValueIntegrityDao.countOrphanSoftDeletedItemValues(),
                    sampleIds = itemValueIntegrityDao.sampleOrphanSoftDeletedItemValues(sampleLimit),
                ),
            scoreProfilesWithoutAttributes =
                DatabaseIntegrityFinding(
                    count = scoreProfileIntegrityDao.countScoreProfilesWithoutAttributes(),
                    sampleIds = scoreProfileIntegrityDao.sampleScoreProfilesWithoutAttributes(sampleLimit),
                ),
            scoreProfileAttributesMissingAttributes =
                DatabaseIntegrityFinding(
                    count = scoreProfileIntegrityDao.countScoreProfileAttributesMissingAttributes(),
                    sampleIds = scoreProfileIntegrityDao.sampleScoreProfileAttributesMissingAttributes(sampleLimit),
                ),
            categoriesWithZeroRankableWeight =
                DatabaseIntegrityFinding(
                    count = categoryIntegrityDao.countCategoriesWithZeroRankableWeight(),
                    sampleIds = categoryIntegrityDao.sampleCategoriesWithZeroRankableWeight(sampleLimit),
                ),
        )
}
