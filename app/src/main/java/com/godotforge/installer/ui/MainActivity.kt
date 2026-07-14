package com.godotforge.installer.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.godotforge.installer.R
import com.godotforge.installer.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let(viewModel::selectFolder)
    }

    private val storageSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        syncFullStorageAccess()
    }

    private val legacyStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        syncFullStorageAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fullAccessButton.setOnClickListener {
            requestFullStorageAccess()
        }

        binding.chooseFolderButton.setOnClickListener {
            folderPicker.launch(viewModel.uiState.value?.selectedUri)
        }

        binding.choosePathButton.setOnClickListener {
            if (hasFullStorageAccess()) {
                showDirectPathDialog()
            } else {
                showFullAccessRequiredDialog()
            }
        }

        binding.refreshButton.setOnClickListener {
            viewModel.refresh()
        }

        binding.installButton.setOnClickListener {
            val state = viewModel.uiState.value ?: return@setOnClickListener
            if (state.addonExists) {
                showReplaceConfirmation()
            } else {
                viewModel.installAddon(replaceExisting = false)
            }
        }

        viewModel.uiState.observe(this, ::render)
    }

    override fun onResume() {
        super.onResume()
        syncFullStorageAccess()
    }

    private fun syncFullStorageAccess() {
        viewModel.updateFullStorageAccess(hasFullStorageAccess())
    }

    private fun hasFullStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestFullStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appSettingsIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            try {
                storageSettingsLauncher.launch(appSettingsIntent)
            } catch (_: ActivityNotFoundException) {
                storageSettingsLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                )
            }
        } else {
            legacyStoragePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
            )
        }
    }

    private fun showDirectPathDialog() {
        val currentPath = viewModel.uiState.value?.selectedPath
            ?: "/storage/emulated/0/Documents"
        val input = EditText(this).apply {
            setText(currentPath)
            hint = getString(R.string.direct_path_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            textDirection = View.TEXT_DIRECTION_LTR
            setSingleLine(true)
            setSelectAllOnFocus(false)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.direct_path_dialog_title)
            .setMessage(R.string.direct_path_dialog_message)
            .setView(input)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.use_path) { _, _ ->
                viewModel.selectPath(input.text?.toString().orEmpty())
            }
            .show()
    }

    private fun showFullAccessRequiredDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.full_access_required_title)
            .setMessage(R.string.full_access_required_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.grant_full_access) { _, _ ->
                requestFullStorageAccess()
            }
            .show()
    }

    private fun showReplaceConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.replace_dialog_title)
            .setMessage(R.string.replace_dialog_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.backup_and_replace) { _, _ ->
                viewModel.installAddon(replaceExisting = true)
            }
            .show()
    }

    private fun render(state: InstallerUiState) = with(binding) {
        fullAccessStatusText.text = if (state.fullStorageAccessGranted) {
            getString(R.string.full_access_granted)
        } else {
            getString(R.string.full_access_not_granted)
        }
        fullAccessButton.text = if (state.fullStorageAccessGranted) {
            getString(R.string.manage_full_access)
        } else {
            getString(R.string.grant_full_access)
        }

        selectedFolderText.text = if (state.folderDisplayName.isBlank()) {
            getString(R.string.no_folder_selected)
        } else {
            state.folderDisplayName
        }

        selectedUriText.text = state.selectedPath
            ?: state.selectedUri?.toString().orEmpty()
        selectedUriText.isVisible = state.hasSelection

        statusText.text = state.statusText
        val statusColor = when (state.statusLevel) {
            StatusLevel.NEUTRAL -> R.color.status_neutral
            StatusLevel.INFO -> R.color.status_info
            StatusLevel.SUCCESS -> R.color.status_success
            StatusLevel.ERROR -> R.color.status_error
        }
        statusText.setTextColor(ContextCompat.getColor(this@MainActivity, statusColor))
        statusCard.strokeColor = ContextCompat.getColor(this@MainActivity, statusColor)

        progressIndicator.isVisible = state.isBusy
        fullAccessButton.isEnabled = !state.isBusy
        chooseFolderButton.isEnabled = !state.isBusy
        choosePathButton.isEnabled = !state.isBusy && state.fullStorageAccessGranted
        refreshButton.isEnabled = !state.isBusy && state.hasSelection
        installButton.isEnabled = !state.isBusy && state.isProjectValid
        installButton.text = if (state.addonExists) {
            getString(R.string.reinstall_addon)
        } else {
            getString(R.string.install_addon)
        }
    }
}
