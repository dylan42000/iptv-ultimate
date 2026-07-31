package com.dylandos.iptv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dylandos.iptv.data.dao.AccountDao
import com.dylandos.iptv.data.entity.AccountEntity
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.data.entity.CategoryEntity
import com.dylandos.iptv.data.repository.ChannelRepository
import com.dylandos.iptv.xtream.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Provides the Paging 3 channel stream for a given content section, scoped to the
 * active account and an optional category filter. Stream is cached so scrolling
 * back doesn't re-query the whole provider.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContentViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val accountDao: AccountDao,
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    data class UiState(
        val account: AccountEntity? = null,
        val section: String = "live",
        val selectedCategoryId: Long? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val accountFlow = accountDao.observeActive()

    fun setSection(section: String) {
        _state.value = _state.value.copy(section = section)
    }

    fun selectCategory(categoryId: Long?) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }

    val channels: Flow<PagingData<ChannelEntity>> = accountFlow.flatMapLatest { account ->
        if (account == null) {
            flowOf(PagingData.empty())
        } else {
            _state.flatMapLatest { st ->
                channelRepository.channelsPaging(
                    account.id,
                    st.section,
                    st.selectedCategoryId
                )
            }
        }
    }.cachedIn(viewModelScope)

    val categories: Flow<List<CategoryEntity>> = accountFlow.flatMapLatest { account ->
        if (account == null) {
            flowOf(emptyList())
        } else {
            _state.flatMapLatest { st ->
                channelRepository.observeCategories(account.id, st.section)
            }
        }
    }
}
