package com.dylandos.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dylandos.iptv.data.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recording: RecordingEntity): Long

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings ORDER BY start_ms DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("UPDATE recordings SET status = :status, end_ms = :endMs, duration_ms = :durationMs, sizeBytes = :sizeBytes WHERE id = :id")
    suspend fun complete(id: Long, status: String, endMs: Long, durationMs: Long, sizeBytes: Long)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun delete(id: Long)
}
