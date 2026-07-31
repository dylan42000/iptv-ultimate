package com.dylandos.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dylandos.iptv.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity): Long

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): AccountEntity?

    @Query("UPDATE accounts SET isActive = 0")
    suspend fun clearActiveFlag()

    @Query("UPDATE accounts SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: Long)

    @Query("UPDATE accounts SET epgUrl = :epgUrl WHERE id = :id")
    suspend fun updateEpgUrl(id: Long, epgUrl: String?)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)

    /** Switches the active account and deactivates all others in one transaction. */
    @Transaction
    suspend fun switchActive(id: Long) {
        clearActiveFlag()
        setActive(id)
    }
}
