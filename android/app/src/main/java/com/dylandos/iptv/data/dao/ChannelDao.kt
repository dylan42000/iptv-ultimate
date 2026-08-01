package com.dylandos.iptv.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dylandos.iptv.data.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(channels: List<ChannelEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(channel: ChannelEntity): Long

    @Query("SELECT * FROM channels WHERE id = :id")
    fun observeById(id: Long): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getById(id: Long): ChannelEntity?

    @Query("SELECT * FROM channels WHERE providerAccountId = :accountId ORDER BY channelNumber ASC")
    fun observeByAccount(accountId: Long): Flow<List<ChannelEntity>>

    @Query(
        "SELECT * FROM channels WHERE providerAccountId = :accountId AND streamType = :type " +
            "AND (:categoryId IS NULL OR categoryId = :categoryId) " +
            "ORDER BY channelNumber ASC"
    )
    fun pagingByType(
        accountId: Long,
        type: String,
        categoryId: Long?
    ): PagingSource<Int, ChannelEntity>

    @Query(
        "SELECT * FROM channels WHERE providerAccountId = :accountId AND streamType = 'live' " +
            "AND (:categoryId IS NULL OR categoryId = :categoryId) AND channelNumber >= :number " +
            "ORDER BY channelNumber ASC LIMIT 1"
    )
    suspend fun nextLiveChannel(
        accountId: Long,
        categoryId: Long?,
        number: Int
    ): ChannelEntity?

    @Query("DELETE FROM channels WHERE providerAccountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)

    @Query("DELETE FROM channels")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM channels WHERE providerAccountId = :accountId")
    fun countByAccount(accountId: Long): Flow<Int>

    @Query("UPDATE channels SET last_watched_pos_ms = :pos WHERE id = :id")
    suspend fun updatePosition(id: Long, pos: Long)
}
