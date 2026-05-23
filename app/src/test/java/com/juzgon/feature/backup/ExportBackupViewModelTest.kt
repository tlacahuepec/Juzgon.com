package com.juzgon.feature.backup

import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupService
import com.juzgon.domain.backup.BackupValidationResult
import com.juzgon.domain.backup.BackupValidator
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExportBackupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var backupService: FakeBackupService
    private lateinit var backupValidator: FakeBackupValidator
    private lateinit var viewModel: ExportBackupViewModel

    @Before
    fun setUp() {
        backupService = FakeBackupService()
        backupValidator = FakeBackupValidator()
        viewModel = ExportBackupViewModel(backupService, backupValidator)
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

    @Test
    fun exportCallsValidatorWithExportedJson() =
        runTest {
            backupService.exportResult = """{"version":2}"""

            viewModel.export()

            assertEquals("""{"version":2}""", backupValidator.lastValidatedJson)
        }

    @Test
    fun exportSetsErrorWhenValidationFails() =
        runTest {
            backupService.exportResult = """{"version":2}"""
            backupValidator.result = BackupValidationResult(listOf("Missing field: app"))

            viewModel.export()

            assertNull(viewModel.state.value.exportedJson)
            assertFalse(viewModel.state.value.isExportComplete)
            assertTrue(
                viewModel.state.value.errorMessage!!
                    .contains("Missing field: app"),
            )
        }

    @Test
    fun exportSetsCompleteOnlyWhenValid() =
        runTest {
            backupService.exportResult = """{"version":2}"""
            backupValidator.result = BackupValidationResult()

            viewModel.export()

            assertTrue(viewModel.state.value.isExportComplete)
            assertEquals("""{"version":2}""", viewModel.state.value.exportedJson)
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

    private class FakeBackupValidator : BackupValidator {
        var result = BackupValidationResult()
        var lastValidatedJson: String? = null

        override fun validate(json: String): BackupValidationResult {
            lastValidatedJson = json
            return result
        }
    }
}
