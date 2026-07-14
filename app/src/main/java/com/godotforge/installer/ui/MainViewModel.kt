package com.godotforge.installer.ui

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.godotforge.installer.R
import com.godotforge.installer.data.GodotProjectRepository
import com.godotforge.installer.data.ProjectSelectionStore
import com.godotforge.installer.domain.InspectionCode
import com.godotforge.installer.domain.InstallResult
import com.godotforge.installer.domain.ProjectInspection
import java.util.concurrent.Executors

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GodotProjectRepository(application)
    private val selectionStore = ProjectSelectionStore(application)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _uiState = MutableLiveData(
        InstallerUiState(statusText = application.getString(R.string.status_choose_folder)),
    )
    val uiState: LiveData<InstallerUiState> = _uiState

    init {
        selectionStore.load()?.let(::inspect)
    }

    fun selectFolder(uri: Uri) {
        val persistResult = repository.persistAccess(uri)
        if (persistResult.isFailure) {
            _uiState.value = InstallerUiState(
                selectedUri = uri,
                statusText = getApplication<Application>().getString(R.string.status_permission_failed),
                statusLevel = StatusLevel.ERROR,
            )
            return
        }

        selectionStore.save(uri)
        inspect(uri)
    }

    fun refresh() {
        _uiState.value?.selectedUri?.let(::inspect)
    }

    fun installAddon(replaceExisting: Boolean) {
        val current = _uiState.value ?: return
        val uri = current.selectedUri ?: return
        if (!current.isProjectValid || current.isBusy) return

        _uiState.value = current.copy(
            isBusy = true,
            statusText = getApplication<Application>().getString(R.string.status_installing),
            statusLevel = StatusLevel.INFO,
        )

        executor.execute {
            val result = repository.install(uri, replaceExisting)
            mainHandler.post {
                val latest = _uiState.value ?: current
                when (result) {
                    is InstallResult.Success -> {
                        val message = result.backupDirectoryName?.let { backupName ->
                            getApplication<Application>().getString(
                                R.string.status_install_success_with_backup,
                                backupName,
                            )
                        } ?: getApplication<Application>().getString(R.string.status_install_success)

                        _uiState.value = latest.copy(
                            isBusy = false,
                            isProjectValid = true,
                            addonExists = true,
                            statusText = message,
                            statusLevel = StatusLevel.SUCCESS,
                        )
                    }

                    InstallResult.ConfirmationRequired -> {
                        _uiState.value = latest.copy(
                            isBusy = false,
                            addonExists = true,
                            statusText = getApplication<Application>().getString(
                                R.string.status_confirmation_required,
                            ),
                            statusLevel = StatusLevel.INFO,
                        )
                    }

                    is InstallResult.Failure -> {
                        val detail = result.technicalMessage.orEmpty().ifBlank {
                            getApplication<Application>().getString(R.string.unknown_error)
                        }
                        _uiState.value = latest.copy(
                            isBusy = false,
                            statusText = getApplication<Application>().getString(
                                R.string.status_install_failed,
                                detail,
                            ),
                            statusLevel = StatusLevel.ERROR,
                        )
                    }
                }
            }
        }
    }

    private fun inspect(uri: Uri) {
        val previous = _uiState.value ?: InstallerUiState()
        _uiState.value = previous.copy(
            selectedUri = uri,
            isBusy = true,
            isProjectValid = false,
            addonExists = false,
            statusText = getApplication<Application>().getString(R.string.status_checking),
            statusLevel = StatusLevel.INFO,
        )

        executor.execute {
            val inspection = repository.inspect(uri)
            mainHandler.post {
                _uiState.value = inspection.toUiState(uri)
            }
        }
    }

    private fun ProjectInspection.toUiState(uri: Uri): InstallerUiState {
        val app = getApplication<Application>()
        val text = when (code) {
            InspectionCode.VALID_NEW -> app.getString(R.string.status_valid_project)
            InspectionCode.VALID_EXISTING -> app.getString(R.string.status_valid_existing)
            InspectionCode.INVALID_TREE -> app.getString(R.string.status_invalid_tree)
            InspectionCode.MISSING_PROJECT_FILE -> app.getString(R.string.status_missing_project_file)
            InspectionCode.NOT_WRITABLE -> app.getString(R.string.status_not_writable)
            InspectionCode.ADDONS_PATH_IS_FILE -> app.getString(R.string.status_addons_is_file)
            InspectionCode.TARGET_PATH_IS_FILE -> app.getString(R.string.status_target_is_file)
            InspectionCode.ACCESS_REVOKED -> app.getString(R.string.status_access_revoked)
            InspectionCode.UNKNOWN_ERROR -> app.getString(
                R.string.status_unknown_error,
                technicalMessage.orEmpty().ifBlank { app.getString(R.string.unknown_error) },
            )
        }

        val level = when (code) {
            InspectionCode.VALID_NEW,
            InspectionCode.VALID_EXISTING -> StatusLevel.SUCCESS

            InspectionCode.INVALID_TREE,
            InspectionCode.MISSING_PROJECT_FILE,
            InspectionCode.NOT_WRITABLE,
            InspectionCode.ADDONS_PATH_IS_FILE,
            InspectionCode.TARGET_PATH_IS_FILE,
            InspectionCode.ACCESS_REVOKED,
            InspectionCode.UNKNOWN_ERROR -> StatusLevel.ERROR
        }

        return InstallerUiState(
            selectedUri = uri,
            folderDisplayName = displayName,
            statusText = text,
            statusLevel = level,
            isBusy = false,
            isProjectValid = isValid,
            addonExists = addonExists,
        )
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }
}
