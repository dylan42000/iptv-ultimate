package com.dylandos.iptv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dylandos.iptv.data.SettingsRepository
import com.dylandos.iptv.data.entity.AccountEntity
import com.dylandos.iptv.data.dao.AccountDao
import com.dylandos.iptv.dvr.DvrStorageManager
import com.dylandos.iptv.xtream.XtreamClient
import com.dylandos.iptv.xtream.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: com.dylandos.iptv.data.AppSettings = com.dylandos.iptv.data.AppSettings(),
    val activeAccount: AccountEntity? = null,
    val storageReady: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val accountDao: AccountDao,
    private val dvrStorageManager: DvrStorageManager,
    private val xtreamRepository: XtreamRepository,
    private val client: XtreamClient
) : ViewModel() {

    private val _storageReady = MutableStateFlow(dvrStorageManager.hasStorage())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        accountDao.observeActive(),
        _storageReady
    ) { s, acc, ready ->
        SettingsUiState(s, acc, ready)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTimeshift(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setTimeshiftEnabled(enabled) }

    fun setTimeshiftMinutes(minutes: Int) =
        viewModelScope.launch { settingsRepository.setTimeshiftMinutes(minutes) }

    fun setDefaultPlayer(player: String) =
        viewModelScope.launch { settingsRepository.setDefaultPlayer(player) }

    fun setHwDecoding(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setHwDecoding(enabled) }

    fun setAudioPassthrough(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setAudioPassthrough(enabled) }

    fun setAutoRecord(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setAutoRecord(enabled) }

    fun onStorageGranted() {
        dvrStorageManager.restorePersistedUri()
        _storageReady.value = dvrStorageManager.hasStorage()
    }

    fun addAccount(name: String, server: String, user: String, pass: String) {
        viewModelScope.launch {
            xtreamRepository.addAccount(
                com.dylandos.iptv.data.entity.AccountEntity(
                    name = name,
                    serverUrl = server,
                    username = user,
                    password = pass,
                    isActive = true
                )
            )
        }
    }

    fun syncEpg() {
        viewModelScope.launch {
            val acc = accountDao.getActive() ?: return@launch
            val epgUrl = acc.epgUrl ?: return@launch
            // EPG refresh handled by EpgRepository via a WorkManager worker in prod.
        }
    }
}
