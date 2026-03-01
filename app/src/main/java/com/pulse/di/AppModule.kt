package com.pulse.di

import androidx.room.Room
import com.pulse.data.db.AppDatabase
import com.pulse.data.services.btr.GoogleAuthManager
import com.pulse.data.services.btr.GoogleDriveBtrService
import com.pulse.data.local.FileStorageManager
import com.pulse.data.local.SettingsManager
import com.pulse.data.repository.LectureRepository
import com.pulse.core.data.repository.NoteRepository
import com.pulse.core.data.repository.NoteVisualRepository
import com.pulse.core.domain.repository.INoteRepository
import com.pulse.domain.services.btr.IBtrAuthManager
import com.pulse.domain.services.btr.IBtrService
import com.pulse.domain.usecase.GetLectureStreamUrlUseCase
import com.pulse.domain.usecase.SyncLecturesUseCase
import com.pulse.presentation.lecture.PlayerProvider
import com.pulse.presentation.library.LibraryViewModel
import com.pulse.presentation.downloads.DownloadsViewModel
import com.pulse.presentation.lecture.LectureViewModel
import com.pulse.core.domain.util.AndroidLogger
import com.pulse.core.domain.util.DefaultFileTypeDetector
import com.pulse.core.domain.util.IFileTypeDetector
import com.pulse.core.domain.util.ILogger
import com.pulse.core.domain.util.NetworkMonitor
import com.pulse.core.domain.util.HlcGenerator
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    
    // Core, Utils & Local
    single<ILogger> { AndroidLogger() }
    single<IFileTypeDetector> { DefaultFileTypeDetector() }
    single { SettingsManager(androidContext()) }
    single { FileStorageManager(androidContext()) }
    single { NetworkMonitor(androidContext()) }
    
    single { 
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "Pulse.db"
        ).fallbackToDestructiveMigration()
         .build()
    }
    
    single { get<AppDatabase>().lectureDao() }
    single { get<AppDatabase>().noteDao() }
    single { get<AppDatabase>().noteVisualDao() }
    
    // HLC Generator for CRDT
    single { 
        val androidId = android.provider.Settings.Secure.getString(
            androidContext().contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: java.util.UUID.randomUUID().toString()
        com.pulse.core.domain.util.HlcGenerator(androidId) 
    }
    
    // Services
    single { GoogleAuthManager(androidContext()) }
    single<IBtrAuthManager> { get<GoogleAuthManager>() }
    
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    single<IBtrService> { GoogleDriveBtrService(get(), get(), get()) }
    
    // UseCases
    single { SyncLecturesUseCase(get(), get(), get()) }
    single { GetLectureStreamUrlUseCase(get(), get()) }
    
    // Repositories
    single<INoteRepository> { NoteRepository(get(), get()) }
    single { NoteVisualRepository(get(), get()) }
    single { LectureRepository(get(), get(), get(), get(), get(), get(), get()) }
    
    // Player — MUST be singleton (SimpleCache uses exclusive DB lock)
    single { PlayerProvider(androidContext(), get()) }
    
    // ViewModels
    viewModel { LibraryViewModel(get(), get(), get()) }
    viewModel { DownloadsViewModel(get()) }
    
    viewModel { (lectureId: String) -> 
        LectureViewModel(lectureId, get(), get(), get(), get(), get(), get(), get()) 
    }
    
    // Workers
    worker { com.pulse.data.services.btr.BtrSyncWorker(get(), get(), get()) }
}
