package com.dylandos.iptv.epg

import com.dylandos.iptv.data.dao.AccountDao
import com.dylandos.iptv.data.dao.ChannelDao
import com.dylandos.iptv.data.dao.ProgramDao
import com.dylandos.iptv.data.entity.ProgramEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the EPG pipeline: fetch -> parse -> multi-pass match -> persist.
 * Exposes normalized programme data consumed by the EPG guide grid.
 */
@Singleton
class EpgRepository @Inject constructor(
    private val channelDao: ChannelDao,
    private val programDao: ProgramDao,
    private val accountDao: AccountDao,
    private val parser: XmltvParser,
    private val matcher: ChannelMatcher
) {

    fun observePrograms(channelId: Long): Flow<List<ProgramEntity>> =
        programDao.observeByChannel(channelId)

    suspend fun currentProgram(channelId: Long): ProgramEntity? =
        programDao.currentProgram(channelId, System.currentTimeMillis())

    suspend fun upcomingPrograms(channelId: Long, limit: Int = 3): List<ProgramEntity> =
        programDao.upcomingPrograms(channelId, System.currentTimeMillis(), limit)

    /**
     * Fetch + parse an EPG XMLTV document and bind its programmes to local
     * channels via the multi-pass matcher. Runs entirely on Dispatchers.IO.
     */
    suspend fun refreshEpg(epgUrl: String): EpgRefreshResult = withContext(Dispatchers.IO) {
        val account = accountDao.getActive() ?: return@withContext EpgRefreshResult(0, 0, 0)

        val channelList = channelDao.observeByAccount(account.id).first()
        val parsed = fetchAndParse(epgUrl)
            ?: return@withContext EpgRefreshResult(0, 0, 0)

        val candidates = channelList.map {
            ChannelMatcher.ChannelCandidate(
                dbId = it.id,
                tvgId = it.tvgId,
                tvgName = it.tvgName,
                displayName = it.name,
                providerName = it.name
            )
        }

        // Map XMLTV channel id -> local DB channel id (three-pass match).
        val idMap = HashMap<String, Long>()
        parsed.channels.forEach { epgCh ->
            matcher.match(epgCh, candidates)?.let { idMap[epgCh.id] = it }
        }

        // Persist matched programmes.
        val programs = ArrayList<ProgramEntity>()
        parsed.programmes.forEach { p ->
            val localId = idMap[p.channelId] ?: return@forEach
            programs.add(
                ProgramEntity(
                    channelId = localId,
                    title = p.title,
                    description = p.description,
                    posterUrl = p.posterUrl,
                    category = p.category,
                    startMs = p.startMs,
                    endMs = p.endMs
                )
            )
        }

        // Replace stale entries to prevent EPG drift.
        programDao.deleteAll()
        programDao.upsertAll(programs)

        EpgRefreshResult(
            channelsParsed = parsed.channels.size,
            programmesParsed = parsed.programmes.size,
            programmesMatched = programs.size
        )
    }

    private fun fetchAndParse(url: String): XmltvResult? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept-Encoding", "identity")
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null

            val encoding = conn.getHeaderField("Content-Encoding")
            if (encoding.equals("gzip", ignoreCase = true)) {
                GZIPInputStream(conn.inputStream).use { parser.parse(it) }
            } else {
                parser.parse(conn.inputStream)
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}

data class EpgRefreshResult(
    val channelsParsed: Int,
    val programmesParsed: Int,
    val programmesMatched: Int
)
