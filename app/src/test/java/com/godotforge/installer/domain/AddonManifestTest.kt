package com.godotforge.installer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddonManifestTest {
    @Test
    fun manifestTargetsExpectedGodotPath() {
        assertEquals("addons", AddonManifest.ADDONS_DIRECTORY)
        assertEquals("godotforge_ai", AddonManifest.TARGET_DIRECTORY)
        assertEquals("project.godot", AddonManifest.PROJECT_MARKER)
    }

    @Test
    fun bundledFilesContainRequiredPluginFiles() {
        val names = AddonManifest.files.map { it.fileName }.toSet()
        assertTrue("plugin.cfg" in names)
        assertTrue("godotforge_ai_plugin.gd" in names)
    }
}
