package com.dylandos.iptv.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dylandos.iptv.data.SettingsRepository
import com.dylandos.iptv.data.entity.AccountEntity
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.data.repository.ChannelRepository
import com.dylandos.iptv.data.dao.AccountDao
import com.dylandos.iptv.ui.navigation.AppSection
import com.dylandos.iptv.ui.navigation.Destination
import com.dylandos.iptv.ui.navigation.NavState
import com.dylandos.iptv.xtream.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Root navigation + section state. Owns the app back stack so that pressing BACK
 * from a detail/player screen restores focus to the exact card that launched it.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val accountDao: AccountDao,
    private val xtreamRepository: XtreamRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _navState = MutableStateFlow(NavState())
    val navState: StateFlow<NavState> = _navState.asStateFlow()

    /** Stack of destinations; top is the current screen. */
    private val backStack = ArrayDeque<Destination>()

    private val _destination = MutableStateFlow<Destination>(Destination.Section)
    val destination: StateFlow<Destination> = _destination.asStateFlow()

    val activeAccount: StateFlow<AccountEntity?> = accountDao.observeActive()

    /** Restore the card focus index when a detail screen pops back. */
    @Volatile var pendingFocusIndex: Int = 0

    fun selectSection(section: AppSection) {
        backStack.clear()
        _destination.value = Destination.Section
        _navState.update { it.copy(section = section, isRailExpanded = false) }
    }

    fun setRailExpanded(expanded: Boolean) {
        _navState.update { it.copy(isRailExpanded = expanded) }
    }

    fun rememberFocus(index: Int) {
        pendingFocusIndex = index
        _navState.update { it.copy(focusRestoreIndex = index) }
    }

    fun openPlayer(channelId: Long) {
        push(Destination.Player(channelId))
    }

    fun openEpg() {
        push(Destination.EpgGuide)
    }

    fun openManageCategories() {
        push(Destination.ManageCategories)
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            backStack.removeLast()
            _destination.value = backStack.lastOrNull() ?: Destination.Section
        } else {
            _destination.value = Destination.Section
        }
    }

    private fun push(dest: Destination) {
        backStack.addLast(dest)
        _destination.value = dest
    }

    fun currentSection(): AppSection = _navState.value.section

    fun channel(id: Long, onResult: (ChannelEntity?) -> Unit) {
        viewModelScope.launch {
            onResult(channelRepository.channelById(id))
        }
    }

    fun purgeAndRefresh() {
        viewModelScope.launch {
            val active = accountDao.getActive() ?: return@launch
            // Guard against cross-account cache leaks when a section reloads.
            channelRepository.purgeAccountData(active.id)
        }
    }
}
