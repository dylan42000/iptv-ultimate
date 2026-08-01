package com.dylandos.iptv.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One EPG program entry, normalized to epoch milliseconds (UTC) at parse time.
 * [startMs]/[endMs] are always stored in UTC; the UI applies local timezone.
 */
@Entity(
    tableName = "programs",
    foreignKeys = [
        ForeignKey(
            entity = ChannelEntity::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["channelId", "startMs"]),
        Index(value = ["channelId"])
    ]
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: Long,
    val title: String,
    val description: String? = null,
    val posterUrl: String? = null,
    val category: String? = null,
    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "end_ms") val endMs: Long,
    @ColumnInfo(name = "external_id") val externalId: String? = null
)
