package com.godotforge.installer.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.godotforge.installer.domain.AddonManifest
import com.godotforge.installer.domain.InspectionCode
import com.godotforge.installer.domain.InstallResult
import com.godotforge.installer.domain.ProjectInspection
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GodotProjectRepository(private val context: Context) {
    private val resolver = context.contentResolver

    fun persistAccess(uri: Uri): Result<Unit> = runCatching {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        resolver.takePersistableUriPermission(uri, flags)
    }

    fun inspect(uri: Uri): ProjectInspection {
        return try {
            val root = DocumentFile.fromTreeUri(context, uri)
                ?: return ProjectInspection(InspectionCode.INVALID_TREE)

            val displayName = root.name.orEmpty().ifBlank { uri.toString() }
            if (!root.exists() || !root.isDirectory) {
                return ProjectInspection(InspectionCode.INVALID_TREE, displayName)
            }

            val projectMarker = root.findFile(AddonManifest.PROJECT_MARKER)
            if (projectMarker == null || !projectMarker.isFile) {
                return ProjectInspection(InspectionCode.MISSING_PROJECT_FILE, displayName)
            }

            if (!root.canRead() || !root.canWrite()) {
                return ProjectInspection(InspectionCode.NOT_WRITABLE, displayName)
            }

            val addons = root.findFile(AddonManifest.ADDONS_DIRECTORY)
            if (addons != null && !addons.isDirectory) {
                return ProjectInspection(InspectionCode.ADDONS_PATH_IS_FILE, displayName)
            }

            val target = addons?.findFile(AddonManifest.TARGET_DIRECTORY)
            if (target != null && !target.isDirectory) {
                return ProjectInspection(InspectionCode.TARGET_PATH_IS_FILE, displayName)
            }

            val addonExists = target?.isDirectory == true
            ProjectInspection(
                code = if (addonExists) InspectionCode.VALID_EXISTING else InspectionCode.VALID_NEW,
                displayName = displayName,
                addonExists = addonExists,
            )
        } catch (error: SecurityException) {
            ProjectInspection(
                code = InspectionCode.ACCESS_REVOKED,
                technicalMessage = error.message,
            )
        } catch (error: Exception) {
            ProjectInspection(
                code = InspectionCode.UNKNOWN_ERROR,
                technicalMessage = error.message,
            )
        }
    }

    fun install(uri: Uri, replaceExisting: Boolean): InstallResult {
        val inspection = inspect(uri)
        if (!inspection.isValid) {
            return InstallResult.Failure(inspection.technicalMessage ?: inspection.code.name)
        }

        return try {
            val root = DocumentFile.fromTreeUri(context, uri)
                ?: return InstallResult.Failure("Unable to resolve selected tree")
            val addons = ensureDirectory(root, AddonManifest.ADDONS_DIRECTORY)
            val existingTarget = addons.findFile(AddonManifest.TARGET_DIRECTORY)
            if (existingTarget != null && !existingTarget.isDirectory) {
                return InstallResult.Failure("Path exists and is not a directory: ${AddonManifest.TARGET_DIRECTORY}")
            }

            if (existingTarget != null && !replaceExisting) {
                return InstallResult.ConfirmationRequired
            }

            val backupName = if (existingTarget != null) {
                val name = uniqueBackupName(addons)
                val backupDirectory = addons.createDirectory(name)
                    ?: return InstallResult.Failure("Unable to create backup directory")
                copyDirectory(existingTarget, backupDirectory)
                name
            } else {
                null
            }

            val target = existingTarget ?: addons.createDirectory(AddonManifest.TARGET_DIRECTORY)
                ?: return InstallResult.Failure("Unable to create addon directory")

            AddonManifest.files.forEach { addonAsset ->
                val targetFile = ensureFile(target, addonAsset.fileName, addonAsset.mimeType)
                val assetPath = "${AddonManifest.ASSET_ROOT}/${addonAsset.fileName}"
                context.assets.open(assetPath).use { input ->
                    openTruncatingOutput(targetFile.uri).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val missingFile = AddonManifest.files.firstOrNull { target.findFile(it.fileName)?.isFile != true }
            if (missingFile != null) {
                return InstallResult.Failure("Installed file is missing: ${missingFile.fileName}")
            }

            InstallResult.Success(backupName)
        } catch (error: Exception) {
            InstallResult.Failure(error.message)
        }
    }

    private fun ensureDirectory(parent: DocumentFile, name: String): DocumentFile {
        require(isSafeName(name)) { "Unsafe directory name" }
        val existing = parent.findFile(name)
        if (existing != null) {
            if (!existing.isDirectory) throw IOException("Path exists and is not a directory: $name")
            return existing
        }
        return parent.createDirectory(name)
            ?: throw IOException("Unable to create directory: $name")
    }

    private fun ensureFile(parent: DocumentFile, name: String, mimeType: String): DocumentFile {
        require(isSafeName(name)) { "Unsafe file name" }
        val existing = parent.findFile(name)
        if (existing != null) {
            if (!existing.isFile) throw IOException("Path exists and is not a file: $name")
            return existing
        }

        val created = parent.createFile(mimeType, name)
            ?: throw IOException("Unable to create file: $name")
        if (created.name != name) {
            throw IOException("Storage provider changed required file name from $name to ${created.name}")
        }
        return created
    }

    private fun copyDirectory(source: DocumentFile, destination: DocumentFile) {
        source.listFiles().forEach { child ->
            val name = child.name ?: throw IOException("A source item has no display name")
            if (!isSafeName(name)) throw IOException("Unsafe source item name: $name")

            if (child.isDirectory) {
                val destinationDirectory = destination.createDirectory(name)
                    ?: throw IOException("Unable to back up directory: $name")
                verifyExactName(destinationDirectory, name)
                copyDirectory(child, destinationDirectory)
            } else if (child.isFile) {
                val destinationFile = destination.createFile(
                    child.type ?: "application/octet-stream",
                    name,
                ) ?: throw IOException("Unable to back up file: $name")
                verifyExactName(destinationFile, name)

                resolver.openInputStream(child.uri).use { input ->
                    if (input == null) throw IOException("Unable to read file: $name")
                    openTruncatingOutput(destinationFile.uri).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun openTruncatingOutput(uri: Uri) =
        try {
            resolver.openOutputStream(uri, "rwt")
        } catch (_: Exception) {
            null
        } ?: resolver.openOutputStream(uri, "w")
        ?: throw IOException("Unable to open destination for writing")

    private fun verifyExactName(document: DocumentFile, expectedName: String) {
        if (document.name != expectedName) {
            throw IOException(
                "Storage provider changed required name from $expectedName to ${document.name}",
            )
        }
    }

    private fun uniqueBackupName(addons: DocumentFile): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val base = "${AddonManifest.TARGET_DIRECTORY}_backup_$stamp"
        var candidate = base
        var counter = 1
        while (addons.findFile(candidate) != null) {
            candidate = "${base}_$counter"
            counter += 1
        }
        return candidate
    }

    private fun isSafeName(name: String): Boolean =
        name.isNotBlank() && name != "." && name != ".." &&
            !name.contains('/') && !name.contains('\\')
}
