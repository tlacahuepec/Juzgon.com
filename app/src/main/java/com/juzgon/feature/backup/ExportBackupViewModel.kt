package com.juzgon.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juzgon.domain.backup.BackupException
import com.juzgon.domain.backup.BackupService
import com.juzgon.domain.backup.BackupValidator
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
        private val backupValidator: BackupValidator,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ExportBackupUiState())

        val state: StateFlow<ExportBackupUiState> = mutableState

        fun export() {
            mutableState.value = ExportBackupUiState(isExporting = true)
            viewModelScope.launch {
                try {
                    val json = backupService.export()
                    val validation = backupValidator.validate(json)
                    if (validation.isValid) {
                        mutableState.value =
                            ExportBackupUiState(
                                exportedJson = json,
                                isExportComplete = true,
                            )
                    } else {
                        mutableState.value =
                            ExportBackupUiState(
                                errorMessage = "Export validation failed: ${validation.errors.first()}",
                            )
                    }
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
