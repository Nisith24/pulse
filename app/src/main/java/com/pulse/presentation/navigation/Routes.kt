package com.pulse.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object LibraryRoute

@Serializable
data class LectureRoute(val lectureId: String)

@Serializable
object SettingsRoute

@Serializable
object DownloadsRoute
