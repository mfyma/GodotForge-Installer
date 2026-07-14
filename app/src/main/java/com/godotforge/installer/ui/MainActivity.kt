package com.godotforge.installer.ui

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.chooseFolderButton.setOnClickListener {
            folderPicker.launch(viewModel.uiState.value?.selectedUri)
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
        selectedFolderText.text = if (state.folderDisplayName.isBlank()) {
            getString(R.string.no_folder_selected)
        } else {
            state.folderDisplayName
        }
        selectedUriText.text = state.selectedUri?.toString().orEmpty()
        selectedUriText.isVisible = state.selectedUri != null

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
        chooseFolderButton.isEnabled = !state.isBusy
        refreshButton.isEnabled = !state.isBusy && state.selectedUri != null
        installButton.isEnabled = !state.isBusy && state.isProjectValid
        installButton.text = if (state.addonExists) {
            getString(R.string.reinstall_addon)
        } else {
            getString(R.string.install_addon)
        }
    }
}
