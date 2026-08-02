package com.dylandos.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dylandos.iptv.data.entity.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(programs: List<ProgramEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(program: ProgramEntity): Long

    @Query("SELECT * FROM programs WHERE channelId = :channelId ORDER BY start_ms ASC")
    fun observeByChannel(channelId: Long): Flow<List<ProgramEntity>>

    @Query(
        "SELECT * FROM programs WHERE channelId = :channelId AND start_ms <= :now " +
            "AND end_ms > :now LIMIT 1"
    )
    suspend fun currentProgram(channelId: Long, now: Long): ProgramEntity?

    @Query(
        "SELECT * FROM programs WHERE channelId = :channelId AND start_ms > :now " +
            "ORDER BY start_ms ASC LIMIT :limit"
    )
    suspend fun upcomingPrograms(channelId: Long, now: Long, limit: Int): List<ProgramEntity>

    @Query("DELETE FROM programs")
    suspend fun deleteAll()

    @Query("DELETE FROM programs WHERE channelId IN (SELECT id FROM channels WHERE providerAccountId = :accountId)")
    suspend fun deleteByAccount(accountId: Long)

    @Query("SELECT COUNT(*) FROM programs")
    fun count(): Flow<Int>
}
