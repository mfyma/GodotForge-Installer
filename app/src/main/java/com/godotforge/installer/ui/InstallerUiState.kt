package com.godotforge.installer.ui

import android.net.Uri

enum class StatusLevel {
    NEUTRAL,
    INFO,
    SUCCESS,
    ERROR,
}

data class InstallerUiState(
    val selectedUri: Uri? = null,
    val folderDisplayName: String = "",
    val statusText: String = "",
    val statusLevel: StatusLevel = StatusLevel.NEUTRAL,
    val isBusy: Boolean = false,
    val isProjectValid: Boolean = false,
    val addonExists: Boolean = false,
)
