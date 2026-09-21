package com.juzgon.feature.backup

import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupService
import com.juzgon.feature.home.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
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
    fun exportProducesArchivePayload() =
        runTest {
            backupService.archiveResult = byteArrayOf(1, 2, 3)

            viewModel.export()

            assertArrayEquals(byteArrayOf(1, 2, 3), viewModel.state.value.exportedArchive)
        }

    @Test
    fun exportSetsSuccessState() =
        runTest {
            backupService.archiveResult = byteArrayOf(1)

            viewModel.export()

            assertTrue(viewModel.state.value.isExportComplete)
        }

    @Test
    fun exportSetsErrorOnFailure() =
        runTest {
            backupService.shouldThrow = true

            viewModel.export()

            assertEquals("Export failed", viewModel.state.value.errorMessage)
            assertNull(viewModel.state.value.exportedArchive)
        }

    @Test
    fun onExportConsumedResetsState() =
        runTest {
            backupService.archiveResult = byteArrayOf(1)
            viewModel.export()

            viewModel.onExportConsumed()

            assertNull(viewModel.state.value.exportedArchive)
            assertEquals(false, viewModel.state.value.isExportComplete)
        }

    @Test
    fun exportCallsArchiveService() =
        runTest {
            backupService.archiveResult = byteArrayOf(1)

            viewModel.export()

            assertEquals(1, backupService.exportArchiveCalls)
        }

    @Test
    fun importWithZipArchiveCallsImportArchive() =
        runTest {
            val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00)

            viewModel.import(zipHeader)

            assertEquals(1, backupService.importArchiveCalls)
            assertTrue(viewModel.state.value.isImportComplete)
        }

    @Test
    fun importWithRawJsonCallsImport() =
        runTest {
            val jsonBytes = """{"app":"Juzgon"}""".toByteArray(Charsets.UTF_8)

            viewModel.import(jsonBytes)

            assertEquals(1, backupService.importJsonCalls)
            assertTrue(viewModel.state.value.isImportComplete)
        }

    @Test
    fun importSetsErrorOnFailure() =
        runTest {
            backupService.shouldThrow = true
            val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

            viewModel.import(zipHeader)

            assertEquals("Operation failed", viewModel.state.value.errorMessage)
            assertEquals(false, viewModel.state.value.isImportComplete)
        }

    @Test
    fun onImportConsumedResetsState() =
        runTest {
            val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
            viewModel.import(zipHeader)

            viewModel.onImportConsumed()

            assertEquals(false, viewModel.state.value.isImportComplete)
            assertNull(viewModel.state.value.errorMessage)
        }

    private class FakeBackupService : BackupService {
        var archiveResult = byteArrayOf()
        var shouldThrow: Boolean = false
        var exportArchiveCalls: Int = 0
        var importArchiveCalls: Int = 0
        var importJsonCalls: Int = 0

        override suspend fun export(): String {
            error("not used")
        }

        override suspend fun exportArchive(): ByteArray {
            exportArchiveCalls += 1
            if (shouldThrow) throw BackupException("Export failed")
            return archiveResult
        }

        override suspend fun importArchive(archive: ByteArray) {
            importArchiveCalls += 1
            if (shouldThrow) throw BackupException("Operation failed")
        }

        override suspend fun import(json: String) {
            importJsonCalls += 1
            if (shouldThrow) throw BackupException("Operation failed")
        }
    }
}
