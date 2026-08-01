package com.dylandos.iptv.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingData
import com.dylandos.iptv.data.dao.CategoryDao
import com.dylandos.iptv.data.dao.ChannelDao
import com.dylandos.iptv.data.entity.CategoryEntity
import com.dylandos.iptv.data.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single repository exposing channels + categories as reactive flows and Paging 3
 * sources. All account switching cascade-purge logic is centralized here so a
 * cache-leak across accounts can never occur.
 */
@Singleton
class ChannelRepository @Inject constructor(
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao
) {

    fun observeChannels(accountId: Long): Flow<List<ChannelEntity>> =
        channelDao.observeByAccount(accountId)

    fun observeCategories(accountId: Long, type: String): Flow<List<CategoryEntity>> =
        categoryDao.observeVisibleByType(accountId, type)

    fun observeAllCategories(accountId: Long): Flow<List<CategoryEntity>> =
        categoryDao.observeAll(accountId)

    suspend fun setCategoryVisibility(id: Long, visible: Boolean) =
        categoryDao.setVisibility(id, visible)

    fun channelsPaging(accountId: Long, type: String, categoryId: Long?): Flow<PagingData<ChannelEntity>> {
        val source: PagingSource<Int, ChannelEntity> =
            channelDao.pagingByType(accountId, type, categoryId)
        return Pager(
            config = PagingConfig(
                pageSize = 60,
                prefetchDistance = 24,
                enablePlaceholders = true,
                initialLoadSize = 120
            ),
            pagingSourceFactory = { source }
        ).flow
    }

    suspend fun channelById(id: Long): ChannelEntity? = channelDao.getById(id)

    fun observeChannelById(id: Long): Flow<ChannelEntity?> = channelDao.observeById(id)

    suspend fun updatePosition(id: Long, pos: Long) = channelDao.updatePosition(id, pos)

    /** Cascade-purge every table scoped to an account to prevent cross-account leakage. */
    suspend fun purgeAccountData(accountId: Long) {
        channelDao.deleteByAccount(accountId)
        categoryDao.deleteByAccount(accountId)
        // Programs are cascade-deleted via FK on channels.
    }
}
