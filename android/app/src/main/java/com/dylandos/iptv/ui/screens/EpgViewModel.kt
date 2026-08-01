package com.dylandos.iptv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dylandos.iptv.data.dao.AccountDao
import com.dylandos.iptv.data.entity.AccountEntity
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.data.entity.ProgramEntity
import com.dylandos.iptv.data.repository.ChannelRepository
import com.dylandos.iptv.epg.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EpgUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val selectedChannel: ChannelEntity? = null,
    val currentProgram: ProgramEntity? = null,
    val upcomingPrograms: List<ProgramEntity> = emptyList(),
    val allPrograms: List<ProgramEntity> = emptyList()
)

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val channelRepository: ChannelRepository,
    private val epgRepository: EpgRepository
) : ViewModel() {

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    private val accountFlow: Flow<AccountEntity?> = accountDao.observeActive()

    val channels: Flow<List<ChannelEntity>> = accountFlow.flatMapLatest {
        if (it == null) flowOf(emptyList())
        else channelRepository.observeChannels(it.id)
    }

    val uiState: Flow<EpgUiState> = combine(
        channels,
        _selectedIndex
    ) { channelList, idx ->
        EpgUiState(channels = channelList, selectedChannel = channelList.getOrNull(idx))
    }.flatMapLatest { state ->
        val selected = state.selectedChannel
        if (selected == null) {
            flowOf(state)
        } else {
            combine(
                epgRepository.observePrograms(selected.id),
                kotlinx.coroutines.flow.flowOf(state)
            ) { programs, st ->
                val now = System.currentTimeMillis()
                val current = programs.firstOrNull { it.startMs <= now && it.endMs > now }
                val upcoming = programs.filter { it.startMs > now }.take(3)
                st.copy(
                    currentProgram = current,
                    upcomingPrograms = upcoming,
                    allPrograms = programs
                )
            }
        }
    }

    fun select(index: Int) {
        _selectedIndex.value = index.coerceAtLeast(0)
    }

    fun refreshEpg() {
        viewModelScope.launch {
            val acc = accountDao.getActive() ?: return@launch
            val epgUrl = acc.epgUrl ?: return@launch
            epgRepository.refreshEpg(epgUrl)
        }
    }
}
