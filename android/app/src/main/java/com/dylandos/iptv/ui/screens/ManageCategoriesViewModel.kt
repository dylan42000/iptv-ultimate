package com.dylandos.iptv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dylandos.iptv.data.dao.AccountDao
import com.dylandos.iptv.data.entity.AccountEntity
import com.dylandos.iptv.data.entity.CategoryEntity
import com.dylandos.iptv.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the "Manage Categories" screen: lists every provider category and lets
 * the user long-press (D-pad) to toggle its [CategoryEntity.isUserVisible] flag.
 */
@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    val categories: Flow<List<CategoryEntity>> = accountDao.observeActive().flatMapLatest {
        if (it == null) flowOf(emptyList())
        else channelRepository.observeAllCategories(it.id)
    }

    fun toggle(category: CategoryEntity) {
        viewModelScope.launch {
            channelRepository.setCategoryVisibility(category.id, !category.isUserVisible)
        }
    }
}
