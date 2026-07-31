package com.dylandos.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dylandos.iptv.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>): List<Long>

    @Query(
        "SELECT * FROM categories WHERE providerAccountId = :accountId AND type = :type " +
            "ORDER BY sortOrder ASC, categoryName ASC"
    )
    fun observeByType(accountId: Long, type: String): Flow<List<CategoryEntity>>

    @Query(
        "SELECT * FROM categories WHERE providerAccountId = :accountId AND type = :type " +
            "AND isUserVisible = 1 ORDER BY sortOrder ASC, categoryName ASC"
    )
    fun observeVisibleByType(accountId: Long, type: String): Flow<List<CategoryEntity>>

    @Query(
        "SELECT * FROM categories WHERE providerAccountId = :accountId " +
            "ORDER BY type ASC, sortOrder ASC"
    )
    fun observeAll(accountId: Long): Flow<List<CategoryEntity>>

    @Query("UPDATE categories SET isUserVisible = :visible WHERE id = :id")
    suspend fun setVisibility(id: Long, visible: Boolean)

    @Query("DELETE FROM categories WHERE providerAccountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
