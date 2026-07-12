package com.studiolash.duplicatecleaner.model

import android.net.Uri

data class Photo(
    val id: Long,
    val uri: Uri,
    val path: String,
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val hash: String
)

data class DuplicateGroup(
    val hash: String,
    val photos: List<Photo>
) {
    val keepPhoto: Photo get() = photos.minByOrNull { it.dateAdded } ?: photos.first()
    val duplicatePhotos: List<Photo> get() = photos.filter { it.id != keepPhoto.id }
    val wastedBytes: Long get() = duplicatePhotos.sumOf { it.size }
}

data class ScanResult(
    val totalScanned: Int,
    val duplicateGroups: List<DuplicateGroup>,
    val totalDuplicates: Int = duplicateGroups.sumOf { it.duplicatePhotos.size },
    val totalWastedBytes: Long = duplicateGroups.sumOf { it.wastedBytes }
)

enum class ScheduleSlot(val label: String, val hour: Int, val minute: Int) {
    MORNING("Morning (8:00 AM)", 8, 0),
    AFTERNOON("Afternoon (2:00 PM)", 14, 0),
    NIGHT("Night (9:00 PM)", 21, 0)
}
