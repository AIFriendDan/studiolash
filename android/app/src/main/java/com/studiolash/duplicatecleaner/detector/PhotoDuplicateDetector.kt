package com.studiolash.duplicatecleaner.detector

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.studiolash.duplicatecleaner.model.DuplicateGroup
import com.studiolash.duplicatecleaner.model.Photo
import com.studiolash.duplicatecleaner.model.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

class PhotoDuplicateDetector(private val context: Context) {

    suspend fun scan(onProgress: (Int, Int) -> Unit = { _, _ -> }): ScanResult =
        withContext(Dispatchers.IO) {
            val photos = queryAllPhotos()
            val groups = mutableMapOf<String, MutableList<Photo>>()
            var processed = 0

            photos.forEach { photo ->
                val hash = computeFileHash(photo.uri)
                if (hash != null) {
                    groups.getOrPut(hash) { mutableListOf() }.add(photo.copy(hash = hash))
                }
                processed++
                onProgress(processed, photos.size)
            }

            val duplicateGroups = groups.values
                .filter { it.size > 1 }
                .map { DuplicateGroup(it.first().hash, it) }

            ScanResult(
                totalScanned = photos.size,
                duplicateGroups = duplicateGroups
            )
        }

    private fun queryAllPhotos(): List<Photo> {
        val photos = mutableListOf<Photo>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATA
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                photos.add(
                    Photo(
                        id = id,
                        uri = uri,
                        path = cursor.getString(dataCol) ?: "",
                        name = cursor.getString(nameCol) ?: "",
                        size = cursor.getLong(sizeCol),
                        dateAdded = cursor.getLong(dateCol),
                        hash = ""
                    )
                )
            }
        }

        return photos
    }

    private fun computeFileHash(uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            var stream: InputStream? = null
            try {
                stream = context.contentResolver.openInputStream(uri) ?: return null
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } finally {
                stream?.close()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deletePhotos(photos: List<Photo>): Int = withContext(Dispatchers.IO) {
        var deleted = 0
        photos.forEach { photo ->
            try {
                val rowsDeleted = context.contentResolver.delete(photo.uri, null, null)
                if (rowsDeleted > 0) deleted++
            } catch (e: Exception) {
                // photo may already be gone
            }
        }
        deleted
    }
}
