package com.juzgon.feature.backup

import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupService
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExportBackupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var backupService: FakeBackupService
    private lateinit var viewModel: ExportBackupViewModel

    @Before
    fun setUp() {
        backupService = FakeBackupService()
        viewModel = ExportBackupViewModel(backupService)
    }

    @Test
    fun exportProducesJsonPayload() =
        runTest {
            backupService.exportResult = """{"version":2}"""

            viewModel.export()

            assertEquals("""{"version":2}""", viewModel.state.value.exportedJson)
        }

    @Test
    fun exportSetsSuccessState() =
        runTest {
            backupService.exportResult = """{"version":2}"""

            viewModel.export()

            assertTrue(viewModel.state.value.isExportComplete)
        }

    @Test
    fun exportSetsErrorOnFailure() =
        runTest {
            backupService.shouldThrow = true

            viewModel.export()

            assertEquals("Export failed", viewModel.state.value.errorMessage)
            assertNull(viewModel.state.value.exportedJson)
        }

    @Test
    fun onExportConsumedResetsState() =
        runTest {
            backupService.exportResult = """{"version":2}"""
            viewModel.export()

            viewModel.onExportConsumed()

            assertNull(viewModel.state.value.exportedJson)
            assertEquals(false, viewModel.state.value.isExportComplete)
        }

    private class FakeBackupService : BackupService {
        var exportResult: String = ""
        var shouldThrow: Boolean = false

        override suspend fun export(): String {
            if (shouldThrow) throw BackupException("Export failed")
            return exportResult
        }

        override suspend fun import(json: String) = error("not used")
    }
}
