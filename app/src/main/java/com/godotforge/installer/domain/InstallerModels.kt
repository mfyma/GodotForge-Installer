package com.godotforge.installer.domain

enum class InspectionCode {
    VALID_NEW,
    VALID_EXISTING,
    INVALID_TREE,
    MISSING_PROJECT_FILE,
    NOT_WRITABLE,
    ADDONS_PATH_IS_FILE,
    TARGET_PATH_IS_FILE,
    ACCESS_REVOKED,
    UNKNOWN_ERROR,
}

data class ProjectInspection(
    val code: InspectionCode,
    val displayName: String = "",
    val addonExists: Boolean = false,
    val technicalMessage: String? = null,
) {
    val isValid: Boolean
        get() = code == InspectionCode.VALID_NEW || code == InspectionCode.VALID_EXISTING
}

sealed interface InstallResult {
    data class Success(val backupDirectoryName: String?) : InstallResult
    data object ConfirmationRequired : InstallResult
    data class Failure(val technicalMessage: String?) : InstallResult
}
