package com.dylandos.iptv.dvr

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB/OTG Storage Priority manager built on the Storage Access Framework (SAF).
 *
 * The user grants a directory tree once via ACTION_OPEN_DOCUMENT_TREE; the URI is
 * persisted with FLAG_GRANT_PERSISTABLE_URI_PERMISSION so we keep write access
 * across reboots without requesting dangerous storage permissions.
 */
@Singleton
class DvrStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREFS = "dvr_storage"
        private const val KEY_TREE_URI = "dvr_tree_uri"
        const val RECORD_DIR = "DylandosDVR"
        const val TIMESHIFT_DIR = "DylandosTimeshift"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The user-selected document-tree URI, or null if not configured. */
    @Volatile var storageTreeUri: Uri? = null
        private set

    fun hasStorage(): Boolean = storageTreeUri != null

    /** Persist the SAF tree URI and request a persistent grant for it. */
    fun setStorageTreeUri(uri: Uri, takePersistablePermission: Boolean) {
        if (takePersistablePermission) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Permission not persistable; fall back to app-foreground access.
            }
        }
        prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
        storageTreeUri = uri
    }

    /** Restore the persisted URI grant after process death. */
    fun restorePersistedUri() {
        val raw = prefs.getString(KEY_TREE_URI, null)
        if (raw != null) {
            val uri = Uri.parse(raw)
            storageTreeUri = uri
            // Re-request flags to refresh the grant on this process.
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
        }
    }

    fun clearStorage() {
        storageTreeUri?.let {
            try {
                context.contentResolver.releasePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
        }
        prefs.edit().remove(KEY_TREE_URI).apply()
        storageTreeUri = null
    }

    /**
     * Create (or retrieve) a child document directory under the granted tree.
     * Falls back to a writable path via StorageManager for USB devices that don't
     * expose a tree but are mounted as FUSE volumes.
     */
    suspend fun getDirectory(subDir: String): File = withContext(Dispatchers.IO) {
        val root = resolveRoot() ?: throw IllegalStateException("DVR storage not configured")
        val dir = File(root, subDir)
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private fun resolveRoot(): File? {
        val uri = storageTreeUri ?: return null
        // Try to map the SAF tree to a real file path (works for mounted volumes).
        val docId = DocumentsContractCompat.getTreeDocumentId(uri)
        val split = docId.split(":")
        return if (split.size > 1) {
            File("/storage/${split[0]}/${split[1]}")
        } else {
            File("/storage/${split[0]}")
        }
    }

    /** Build a SAF content URI for a file inside a tree sub-directory. */
    fun buildDocumentUri(subDir: String, fileName: String): Uri? {
        val uri = storageTreeUri ?: return null
        val dirDoc = DocumentsContractCompat.buildChildDocumentsUriUsingTree(uri, subDir)
        return DocumentsContractCompat.buildDocumentUriUsingTree(uri, "$subDir/$fileName")
    }

    fun queryDocumentSize(uri: Uri): Long {
        var size = 0L
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val idx = it.getColumnIndex(OpenableColumns.SIZE)
            if (idx >= 0 && it.moveToFirst()) size = it.getLong(idx)
        }
        return size
    }

    fun isUsbMounted(): Boolean {
        val path = resolveRoot()
        if (path != null && path.exists()) return true
        // Query mounted external volumes through StorageManager.
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as? android.os.storage.StorageManager
            ?: return false
        return try {
            sm.storageVolumes.any { vol ->
                vol.state == android.os.Environment.MEDIA_MOUNTED && vol.isRemovable
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Open the OS storage settings screen so users can choose a USB drive. */
    fun openStorageSettingsIntent(): Intent = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
}

/** Minimal DocumentsContract helpers (avoids pulling in the full androidx documentfile). */
object DocumentsContractCompat {
    fun getTreeDocumentId(uri: Uri): String =
        android.provider.DocumentsContract.getTreeDocumentId(uri)

    fun buildChildDocumentsUriUsingTree(treeUri: Uri, subDir: String): Uri =
        android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            subDir
        )

    fun buildDocumentUriUsingTree(treeUri: Uri, documentId: String): Uri =
        android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
}
