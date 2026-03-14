package com.pulse.presentation.customlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.core.data.db.CustomList
import com.pulse.core.data.db.Lecture
import com.pulse.data.repository.LectureRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomListViewModel(
    private val repository: LectureRepository
) : ViewModel() {

    val customLists: StateFlow<List<CustomList>> = repository.getAllCustomLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedLectures: StateFlow<List<Lecture>> = repository.completedLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentListLectures = MutableStateFlow<List<Lecture>>(emptyList())
    val currentListLectures = _currentListLectures.asStateFlow()

    fun loadLecturesForList(listId: Long) {
        viewModelScope.launch {
            repository.getLecturesForCustomList(listId).collect {
                _currentListLectures.value = it
            }
        }
    }

    fun createList(name: String, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.createCustomList(name)
            onCreated?.invoke(id)
        }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch {
            repository.deleteCustomList(listId)
        }
    }

    fun addLectureToList(listId: Long, lectureId: String) {
        viewModelScope.launch {
            repository.addLectureToCustomList(listId, lectureId)
        }
    }

    fun removeLectureFromList(listId: Long, lectureId: String) {
        viewModelScope.launch {
            repository.removeLectureFromCustomList(listId, lectureId)
        }
    }

    fun markAsCompleted(lectureId: String) {
        viewModelScope.launch {
            repository.markAsCompleted(lectureId)
        }
    }

    fun resetProgress(lectureId: String) {
        viewModelScope.launch {
            repository.resetProgress(lectureId)
        }
    }
}
