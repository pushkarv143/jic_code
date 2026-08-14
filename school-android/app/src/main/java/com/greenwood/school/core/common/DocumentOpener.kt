package com.greenwood.school.core.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a binary endpoint's [ResponseBody] (receipt PDF, salary slip, ID card,
 * report card, Excel export) into a file the user can open or share.
 *
 * The web app triggers a browser download; on Android the equivalent that actually
 * works across OEMs is: write to the app cache, expose via FileProvider, hand the
 * content:// URI to whatever app claims the MIME type.
 */
@Singleton
class DocumentOpener @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Writes the body to cache and returns the file, or null if writing failed. */
    suspend fun save(body: ResponseBody, fileName: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, DOCUMENTS_DIR).apply { mkdirs() }
            val file = File(directory, fileName.sanitised())
            body.byteStream().use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        }.getOrNull()
    }

    /**
     * Opens the file in an external viewer.
     *
     * @return false when no installed app can handle the type, so the caller can
     *         show "No app available to open this file" instead of failing silently.
     */
    fun open(file: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.mimeType())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun share(file: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file.mimeType()
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun File.mimeType(): String = when (extension.lowercase()) {
        "pdf" -> "application/pdf"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "csv" -> "text/csv"
        else -> "*/*"
    }

    private fun String.sanitised(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        // Must match res/xml/file_paths.xml.
        const val DOCUMENTS_DIR = "documents"
    }
}
