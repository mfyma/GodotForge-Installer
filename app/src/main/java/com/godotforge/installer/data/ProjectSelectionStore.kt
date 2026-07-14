package com.godotforge.installer.data

import android.content.Context
import android.net.Uri

class ProjectSelectionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(uri: Uri) {
        preferences.edit()
            .putString(KEY_PROJECT_URI, uri.toString())
            .remove(KEY_PROJECT_PATH)
            .apply()
    }

    fun savePath(path: String) {
        preferences.edit()
            .putString(KEY_PROJECT_PATH, path)
            .remove(KEY_PROJECT_URI)
            .apply()
    }

    fun loadUri(): Uri? = preferences.getString(KEY_PROJECT_URI, null)?.let(Uri::parse)

    fun loadPath(): String? = preferences.getString(KEY_PROJECT_PATH, null)

    fun clear() {
        preferences.edit()
            .remove(KEY_PROJECT_URI)
            .remove(KEY_PROJECT_PATH)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "godotforge_installer"
        const val KEY_PROJECT_URI = "project_tree_uri"
        const val KEY_PROJECT_PATH = "project_direct_path"
    }
}
