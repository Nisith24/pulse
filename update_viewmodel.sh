sed -i.bak '/val localLectures = combine(/,/initialValue = emptyList()/,/    )/ {
    /    )/a\
\
    val recentLocalLectures = localLectures.combine(repository.recentLecture) { lectures, _ ->\
        lectures.sortedByDescending { it.updatedAt.takeIf { t -> t > 0 } ?: it.lastPosition }\
            .take(4)\
    }.stateIn(\
        scope = viewModelScope,\
        started = SharingStarted.WhileSubscribed(5000),\
        initialValue = emptyList()\
    )\
\
    val videoLocalLectures = localLectures.combine(recentLocalLectures) { lectures, recent ->\
        lectures.filter { !it.videoLocalPath.isNullOrEmpty() && it.id !in recent.map { r -> r.id } }\
    }.stateIn(\
        scope = viewModelScope,\
        started = SharingStarted.WhileSubscribed(5000),\
        initialValue = emptyList()\
    )\
\
    val pdfLocalLectures = localLectures.combine(recentLocalLectures) { lectures, recent ->\
        lectures.filter { it.pdfLocalPath.isNotEmpty() && it.id !in recent.map { r -> r.id } }\
    }.stateIn(\
        scope = viewModelScope,\
        started = SharingStarted.WhileSubscribed(5000),\
        initialValue = emptyList()\
    )
}' ./app/src/main/java/com/pulse/presentation/library/LibraryViewModel.kt
