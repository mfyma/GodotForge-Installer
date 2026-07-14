package com.godotforge.installer.domain

data class AddonAsset(
    val fileName: String,
    val mimeType: String,
)

object AddonManifest {
    const val ASSET_ROOT = "godotforge_ai"
    const val ADDONS_DIRECTORY = "addons"
    const val TARGET_DIRECTORY = "godotforge_ai"
    const val PROJECT_MARKER = "project.godot"

    val files = listOf(
        AddonAsset("plugin.cfg", "application/octet-stream"),
        AddonAsset("godotforge_ai_plugin.gd", "application/octet-stream"),
        AddonAsset("README.md", "text/markdown"),
    )
}
