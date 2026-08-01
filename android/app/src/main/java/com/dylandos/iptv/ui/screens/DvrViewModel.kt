package com.dylandos.iptv.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dylandos.iptv.data.dao.RecordingDao
import com.dylandos.iptv.data.entity.RecordingEntity
import com.dylandos.iptv.dvr.DvrStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DvrViewModel @Inject constructor(
    private val recordingDao: RecordingDao,
    private val dvrStorageManager: DvrStorageManager
) : ViewModel() {

    private val _storageReady = MutableStateFlow(false)
    val storageReady: StateFlow<Boolean> = _storageReady.asStateFlow()

    val recordings: Flow<List<RecordingEntity>> = recordingDao.observeAll()

    init {
        viewModelScope.launch {
            _storageReady.value = dvrStorageManager.hasStorage()
        }
    }

    fun onStorageGranted() {
        dvrStorageManager.restorePersistedUri()
        _storageReady.value = dvrStorageManager.hasStorage()
    }

    fun deleteRecording(id: Long) {
        viewModelScope.launch {
            recordingDao.delete(id)
        }
    }
}
