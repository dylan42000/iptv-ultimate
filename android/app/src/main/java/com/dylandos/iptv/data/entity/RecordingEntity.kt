package com.dylandos.iptv.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata for a completed or in-progress DVR recording. [uri] is a persisted
 * SAF content URI string so recordings survive process death and are accessible
 * via the Storage Access Framework.
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    val channelName: String,
    val title: String,
    val uri: String,
    val fileName: String,
    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "end_ms") val endMs: Long? = null,
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val status: String = "RECORDING",   // RECORDING | COMPLETE | FAILED
    val partIndex: Int = 1,
    val thumbUri: String? = null
)
