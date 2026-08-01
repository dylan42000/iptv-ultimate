package com.dylandos.iptv.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single channel (Live TV, VOD, or Series entry) imported from an Xtream API
 * or M3U playlist. The identity of the channel within a provider account is
 * scoped by [providerAccountId] so that switching accounts can never leak data.
 */
@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["providerAccountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["channelNumber"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerAccountId: Long,
    val streamId: String,
    val streamType: String,               // "live", "movie", "series", "vod"
    val name: String,
    val tvgId: String? = null,            // exact tvg-id from EPG
    val tvgName: String? = null,          // exact tvg-name from EPG
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val url: String,
    val logoUrl: String? = null,
    val backdropUrl: String? = null,
    val channelNumber: Int = 0,
    val container: String? = null,        // "mpegts", "m3u8", "mp4", "mkv", ...
    val isSeries: Boolean = false,
    val season: Int? = null,
    val episode: Int? = null,
    @ColumnInfo(name = "last_watched_pos_ms") val lastWatchedPosMs: Long = 0L
)
