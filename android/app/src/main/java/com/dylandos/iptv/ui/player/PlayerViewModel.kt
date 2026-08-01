package com.dylandos.iptv.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.data.entity.ProgramEntity
import com.dylandos.iptv.data.repository.ChannelRepository
import com.dylandos.iptv.epg.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiData(
    val channel: ChannelEntity? = null,
    val currentProgram: ProgramEntity? = null,
    val nextPrograms: List<ProgramEntity> = emptyList()
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _channel = MutableStateFlow<ChannelEntity?>(null)

    /** Bind this player to a channel id (called from the Player screen). */
    fun setChannel(channelId: Long) {
        if (_channel.value?.id == channelId) return
        viewModelScope.launch {
            _channel.value = channelRepository.channelById(channelId)
        }
    }

    val data: Flow<PlayerUiData> = _channel.flatMapLatest { ch ->
        if (ch == null) {
            flowOf(PlayerUiData())
        } else {
            epgRepository.observePrograms(ch.id).let { programsFlow ->
                combine(programsFlow, _channel) { programs, c ->
                    val now = System.currentTimeMillis()
                    PlayerUiData(
                        channel = c,
                        currentProgram = programs.firstOrNull { it.startMs <= now && it.endMs > now },
                        nextPrograms = programs.filter { it.startMs > now }.take(3)
                    )
                }
            }
        }
    }

    fun observeChannel(): Flow<ChannelEntity?> = _channel
}
