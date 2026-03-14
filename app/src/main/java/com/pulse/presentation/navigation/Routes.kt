package com.pulse.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object LibraryRoute

@Serializable
data class LectureRoute(val lectureId: String, val sourceFolderId: String? = null)

@Serializable
object SettingsRoute

@Serializable
object DownloadsRoute

@Serializable
object SubjectsRoute

@Serializable
data class SubjectDetailRoute(val subjectName: String)

@Serializable
object PrepladderRRRoute

@Serializable
object CustomListsRoute

@Serializable
object CompletedRoute

@Serializable
data class CustomListDetailRoute(val listId: Long, val listName: String)
