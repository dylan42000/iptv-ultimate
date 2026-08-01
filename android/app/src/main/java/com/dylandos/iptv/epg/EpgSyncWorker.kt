package com.dylandos.iptv.epg

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dylandos.iptv.data.dao.AccountDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic EPG refresh worker. Enqueued via WorkManager so XMLTV parsing and
 * channel matching happen off the UI thread and survive process death.
 */
@HiltWorker
class EpgSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val accountDao: AccountDao,
    private val epgRepository: EpgRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val account = accountDao.getActive() ?: return Result.success()
        val epgUrl = account.epgUrl ?: return Result.retry()
        return try {
            val result = epgRepository.refreshEpg(epgUrl)
            if (result.programmesParsed > 0) Result.success()
            else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "epg_sync"
    }
}
