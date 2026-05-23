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
    val exportedJson: String? = null,
    val errorMessage: String? = null,
    val isExportComplete: Boolean = false,
)

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
                    val json = backupService.export()
                    mutableState.value =
                        ExportBackupUiState(
                            exportedJson = json,
                            isExportComplete = true,
                        )
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
    }
