package com.juzgon.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExportBackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportedArchive: ByteArray? = null,
    val errorMessage: String? = null,
    val isExportComplete: Boolean = false,
    val isImportComplete: Boolean = false,
)

private const val MIN_ZIP_HEADER_SIZE = 4
private const val ZIP_MAGIC_0 = 0x50.toByte()
private const val ZIP_MAGIC_1 = 0x4B.toByte()
private const val ZIP_MAGIC_2 = 0x03.toByte()
private const val ZIP_MAGIC_3 = 0x04.toByte()
private const val BYTE_INDEX_0 = 0
private const val BYTE_INDEX_1 = 1
private const val BYTE_INDEX_2 = 2
private const val BYTE_INDEX_3 = 3

@HiltViewModel
class ExportBackupViewModel
    @Inject
    constructor(
        private val backupService: BackupService,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ExportBackupUiState())

        val state: StateFlow<ExportBackupUiState> = mutableState

        fun export() {
            mutableState.value = ExportBackupUiState(isExporting = true)
            viewModelScope.launch {
                try {
                    val archive = backupService.exportArchive()
                    mutableState.value = ExportBackupUiState(exportedArchive = archive, isExportComplete = true)
                } catch (e: BackupException) {
                    mutableState.value =
                        ExportBackupUiState(
                            errorMessage = e.message ?: "Export failed",
                        )
                }
            }
        }

        fun onExportConsumed() {
            mutableState.value = ExportBackupUiState()
        }

        fun onImportConsumed() {
            mutableState.value = ExportBackupUiState()
        }

        @Suppress("TooGenericExceptionCaught")
        fun import(fileBytes: ByteArray) {
            mutableState.value = ExportBackupUiState(isImporting = true)
            viewModelScope.launch {
                try {
                    if (isZipArchive(fileBytes)) {
                        backupService.importArchive(fileBytes)
                    } else {
                        backupService.import(fileBytes.toString(Charsets.UTF_8))
                    }
                    mutableState.value = ExportBackupUiState(isImportComplete = true)
                } catch (e: Exception) {
                    mutableState.value =
                        ExportBackupUiState(
                            errorMessage = e.message ?: "Import failed",
                        )
                }
            }
        }

        private fun isZipArchive(bytes: ByteArray): Boolean =
            bytes.size >= MIN_ZIP_HEADER_SIZE &&
                bytes[BYTE_INDEX_0] == ZIP_MAGIC_0 &&
                bytes[BYTE_INDEX_1] == ZIP_MAGIC_1 &&
                bytes[BYTE_INDEX_2] == ZIP_MAGIC_2 &&
                bytes[BYTE_INDEX_3] == ZIP_MAGIC_3
    }
