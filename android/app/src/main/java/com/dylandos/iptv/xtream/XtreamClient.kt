package com.dylandos.iptv.xtream

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** DTO for a provider category. */
data class XtreamCategory(
    val categoryId: String,
    val categoryName: String,
    val parentId: Long?,
    val type: String
)

/** DTO for a live / VOD / series entry. */
data class XtreamItem(
    val name: String,
    val streamType: String,
    val streamId: String,
    val categoryId: String?,
    val icon: String?,
    val backdrop: String?,
    val containerExtension: String?,
    val epgChannelId: String?,
    val rating: String?,
    val plot: String?,
    val genre: String?,
    val channelNumber: Int,
    val isSeries: Boolean
)

/**
 * Minimal Xtream Codes HTTP client using android's built-in org.json parser to
 * keep the dependency surface (and DEX count) low for a 2 GB device.
 */
@Singleton
class XtreamClient @Inject constructor() {

    companion object {
        private const val TIMEOUT_MS = 15_000
    }

    data class Credentials(
        val serverUrl: String,
        val username: String,
        val password: String
    )

    private fun getJson(credentials: Credentials, action: String): JSONArray? {
        val base = credentials.serverUrl.trimEnd('/')
        val url = "$base/player_api.php?username=${credentials.username}" +
            "&password=${credentials.password}&action=$action"
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Connection", "Keep-Alive")
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            // Action endpoints return a top-level JSON array, e.g. "[{...}]".
            parseResponse(text)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseResponse(text: String): JSONArray? {
        // Some providers wrap in an object; handle both shapes robustly.
        if (text.trimStart().startsWith("[")) {
            return try {
                JSONArray(text)
            } catch (_: Exception) {
                null
            }
        }
        return try {
            val root = JSONObject(text)
            root.optJSONArray("live_streams")
                ?: root.optJSONArray("vod_streams")
                ?: root.optJSONArray("series")
                ?: root.optJSONArray("categories")
        } catch (_: Exception) {
            null
        }
    }

    fun fetchLiveCategories(credentials: Credentials): List<XtreamCategory> =
        parseCategories(getJson(credentials, "get_live_categories"), "live")

    fun fetchVodCategories(credentials: Credentials): List<XtreamCategory> =
        parseCategories(getJson(credentials, "get_vod_categories"), "movie")

    fun fetchSeriesCategories(credentials: Credentials): List<XtreamCategory> =
        parseCategories(getJson(credentials, "get_series_categories"), "series")

    fun fetchLiveStreams(credentials: Credentials): List<XtreamItem> =
        parseStreams(getJson(credentials, "get_live_streams"))

    fun fetchVodStreams(credentials: Credentials): List<XtreamItem> =
        parseStreams(getJson(credentials, "get_vod_streams"))

    fun fetchSeries(credentials: Credentials): List<XtreamItem> =
        parseStreams(getJson(credentials, "get_series"))

    /** Build the playable stream URL for an item. */
    fun buildStreamUrl(credentials: Credentials, item: XtreamItem): String {
        val base = credentials.serverUrl.trimEnd('/')
        return when {
            item.isSeries -> {
                val ext = item.containerExtension ?: "mp4"
                "$base/series/${credentials.username}/${credentials.password}/${item.streamId}.$ext"
            }
            item.streamType == "movie" || item.streamType == "vod" -> {
                val ext = item.containerExtension ?: "mp4"
                "$base/movie/${credentials.username}/${credentials.password}/${item.streamId}.$ext"
            }
            else -> {
                // Live defaults to HLS (.m3u8) unless the provider reports a container.
                val ext = item.containerExtension?.takeIf { it.isNotBlank() } ?: "m3u8"
                "$base/live/${credentials.username}/${credentials.password}/${item.streamId}.$ext"
            }
        }
    }

    private fun parseCategories(arr: JSONArray?, type: String): List<XtreamCategory> {
        val out = ArrayList<XtreamCategory>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                XtreamCategory(
                    categoryId = o.optString("category_id"),
                    categoryName = o.optString("category_name"),
                    parentId = o.optLong("parent_id", -1).takeIf { it >= 0 },
                    type = type
                )
            )
        }
        return out
    }

    private fun parseStreams(arr: JSONArray?): List<XtreamItem> {
        val out = ArrayList<XtreamItem>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val streamId = o.optString("stream_id").ifBlank { o.optString("series_id") }
            if (streamId.isBlank()) continue
            val type = o.optString("stream_type")
            val isSeries = o.optString("series_id").isNotBlank()

            var backdrop: String? = o.optString("backdrop_path").takeIf { it.isNotBlank() && it != "null" }
            if (backdrop == null) {
                val arrBp = o.optJSONArray("backdrop_path")
                if (arrBp != null && arrBp.length() > 0) {
                    backdrop = arrBp.optString(0)
                }
            }

            out.add(
                XtreamItem(
                    name = o.optString("name"),
                    streamType = if (isSeries) "series" else type,
                    streamId = streamId,
                    categoryId = o.optString("category_id").takeIf { it.isNotBlank() && it != "0" },
                    icon = o.optString("stream_icon").takeIf { it.isNotBlank() },
                    backdrop = backdrop,
                    containerExtension = o.optString("container_extension").takeIf { it.isNotBlank() },
                    epgChannelId = o.optString("epg_channel_id").takeIf { it.isNotBlank() },
                    rating = o.optString("rating").takeIf { it.isNotBlank() && it != "null" },
                    plot = o.optString("plot").takeIf { it.isNotBlank() && it != "null" },
                    genre = o.optString("genre").takeIf { it.isNotBlank() && it != "null" },
                    channelNumber = o.optInt("num", 0),
                    isSeries = isSeries
                )
            )
        }
        return out
    }
}
