package com.juzgon.data.local

import com.juzgon.data.local.dao.DatabaseIntegrityDao

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

class DatabaseIntegrityRepository(
    private val dao: DatabaseIntegrityDao,
    private val sampleLimit: Int = DEFAULT_SAMPLE_LIMIT,
) {
    suspend fun diagnose(): DatabaseIntegrityReport =
        DatabaseIntegrityReport(
            orphanRatings =
                DatabaseIntegrityFinding(
                    count = dao.countOrphanRatings(),
                    sampleIds = dao.sampleOrphanRatings(sampleLimit),
                ),
            orphanActiveItemValues =
                DatabaseIntegrityFinding(
                    count = dao.countOrphanActiveItemValues(),
                    sampleIds = dao.sampleOrphanActiveItemValues(sampleLimit),
                ),
            orphanSoftDeletedItemValues =
                DatabaseIntegrityFinding(
                    count = dao.countOrphanSoftDeletedItemValues(),
                    sampleIds = dao.sampleOrphanSoftDeletedItemValues(sampleLimit),
                ),
            scoreProfilesWithoutAttributes =
                DatabaseIntegrityFinding(
                    count = dao.countScoreProfilesWithoutAttributes(),
                    sampleIds = dao.sampleScoreProfilesWithoutAttributes(sampleLimit),
                ),
            scoreProfileAttributesMissingAttributes =
                DatabaseIntegrityFinding(
                    count = dao.countScoreProfileAttributesMissingAttributes(),
                    sampleIds = dao.sampleScoreProfileAttributesMissingAttributes(sampleLimit),
                ),
            categoriesWithZeroRankableWeight =
                DatabaseIntegrityFinding(
                    count = dao.countCategoriesWithZeroRankableWeight(),
                    sampleIds = dao.sampleCategoriesWithZeroRankableWeight(sampleLimit),
                ),
        )
}
