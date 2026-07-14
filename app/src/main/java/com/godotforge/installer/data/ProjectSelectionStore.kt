package com.godotforge.installer.data

import android.content.Context
import android.net.Uri

class ProjectSelectionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(uri: Uri) {
        preferences.edit().putString(KEY_PROJECT_URI, uri.toString()).apply()
    }

    fun load(): Uri? = preferences.getString(KEY_PROJECT_URI, null)?.let(Uri::parse)

    fun clear() {
        preferences.edit().remove(KEY_PROJECT_URI).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "godotforge_installer"
        const val KEY_PROJECT_URI = "project_tree_uri"
    }
}
