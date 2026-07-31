package com.dylandos.iptv.xtream

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.dylandos.iptv.data.dao.AccountDao
import com.dylandos.iptv.data.dao.CategoryDao
import com.dylandos.iptv.data.dao.ChannelDao
import com.dylandos.iptv.data.dao.ProgramDao
import com.dylandos.iptv.data.entity.AccountEntity
import com.dylandos.iptv.data.entity.CategoryEntity
import com.dylandos.iptv.data.entity.ChannelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Xtream / M3U catalog ingestion with Paging 3 for large providers and
 * strict account isolation (mandatory cascade purge on account switch/logout).
 */
@Singleton
class XtreamRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao,
    private val programDao: ProgramDao,
    private val client: XtreamClient
) {

    /** Stream for paged channels of a given type/category. */
    fun pagedChannels(
        accountId: Long,
        type: String,
        categoryId: Long?
    ): Flow<PagingData<ChannelEntity>> = channelDao.pagingByType(
        accountId, type, categoryId
    ).let { source ->
        Pager(
            config = PagingConfig(pageSize = 60, prefetchDistance = 24, enablePlaceholders = true),
            pagingSourceFactory = { source }
        ).flow
    }

    suspend fun refreshCatalog(credentials: XtreamClient.Credentials, accountId: Long): SyncSummary =
        withContext(Dispatchers.IO) {
            val categories = buildCategories(credentials, accountId)
            persistCategories(categories, accountId)

            val items = ArrayList<XtreamItem>()
            items += client.fetchLiveStreams(credentials)
            items += client.fetchVodStreams(credentials)
            items += client.fetchSeries(credentials)

            persistChannels(credentials, items, accountId, categories)

            SyncSummary(
                categories = categories.size,
                channels = items.size
            )
        }

    private fun buildCategories(
        credentials: XtreamClient.Credentials,
        accountId: Long
    ): List<CategoryEntity> {
        val out = ArrayList<CategoryEntity>()
        out += client.fetchLiveCategories(credentials).map {
            CategoryEntity(
                providerAccountId = accountId,
                categoryId = it.categoryId,
                categoryName = it.categoryName,
                parentId = it.parentId,
                type = "live"
            )
        }
        out += client.fetchVodCategories(credentials).map {
            CategoryEntity(
                providerAccountId = accountId,
                categoryId = it.categoryId,
                categoryName = it.categoryName,
                parentId = it.parentId,
                type = "movie"
            )
        }
        out += client.fetchSeriesCategories(credentials).map {
            CategoryEntity(
                providerAccountId = accountId,
                categoryId = it.categoryId,
                categoryName = it.categoryName,
                parentId = it.parentId,
                type = "series"
            )
        }
        return out.distinctBy { it.categoryId to it.type }
    }

    private suspend fun persistCategories(categories: List<CategoryEntity>, accountId: Long) {
        categoryDao.deleteByAccount(accountId)
        categoryDao.upsertAll(categories.mapIndexed { i, c -> c.copy(sortOrder = i) })
    }

    private suspend fun persistChannels(
        credentials: XtreamClient.Credentials,
        items: List<XtreamItem>,
        accountId: Long,
        categories: List<CategoryEntity>
    ) {
        // Map provider category id -> local category id.
        val catIdByKey = categories
            .groupBy { "${it.type}:${it.categoryId}" }
            .mapValues { it.value.first().id }

        channelDao.deleteByAccount(accountId)

        val channels = items.map { item ->
            val type = when {
                item.isSeries -> "series"
                item.streamType == "movie" || item.streamType == "vod" -> "movie"
                else -> "live"
            }
            val localCatId = item.categoryId?.let { catIdByKey["$type:$it"] }
            ChannelEntity(
                providerAccountId = accountId,
                streamId = item.streamId,
                streamType = type,
                name = item.name,
                tvgId = item.epgChannelId,
                tvgName = item.epgChannelId,
                categoryId = localCatId,
                categoryName = localCatId?.let { lid ->
                    categories.firstOrNull { it.id == lid }?.categoryName
                },
                url = client.buildStreamUrl(credentials, item),
                logoUrl = item.icon,
                backdropUrl = item.backdrop,
                channelNumber = item.channelNumber,
                container = item.containerExtension,
                isSeries = item.isSeries
            )
        }
        channelDao.upsertAll(channels)
    }

    /**
     * Account isolation: purge every local table scoped to the account so a later
     * account can never observe stale channels/EPG/categories.
     */
    suspend fun purgeAccount(accountId: Long) {
        programDao.deleteByAccount(accountId)
        channelDao.deleteByAccount(accountId)
        categoryDao.deleteByAccount(accountId)
    }

    /** Switch active account and cascade-purge the outgoing account's cache. */
    suspend fun switchAccount(newActive: AccountEntity) {
        val old = accountDao.getActive()
        accountDao.switchActive(newActive.id)
        if (old != null && old.id != newActive.id) {
            purgeAccount(old.id)
        }
    }

    suspend fun addAccount(account: AccountEntity): Long {
        val id = accountDao.insert(account)
        accountDao.switchActive(id)
        return id
    }

    suspend fun logout() {
        val active = accountDao.getActive() ?: return
        accountDao.clearActiveFlag()
        purgeAccount(active.id)
    }
}

data class SyncSummary(
    val categories: Int,
    val channels: Int
)
