package com.pulse.di

import androidx.room.Room
import com.pulse.data.db.AppDatabase
import com.pulse.data.drive.DriveAuthManager
import com.pulse.data.drive.DriveService
import com.pulse.data.local.FileStorageManager
import com.pulse.data.local.SettingsManager
import com.pulse.data.repository.LectureRepository
import com.pulse.data.repository.NoteRepository
import com.pulse.domain.repository.INoteRepository
import com.pulse.domain.service.IDriveAuthManager
import com.pulse.domain.service.IDriveService
import com.pulse.domain.usecase.GetLectureStreamUrlUseCase
import com.pulse.domain.usecase.SyncLecturesUseCase
import com.pulse.domain.util.AndroidLogger
import com.pulse.domain.util.DefaultFileTypeDetector
import com.pulse.domain.util.IFileTypeDetector
import com.pulse.domain.util.ILogger
import com.pulse.presentation.lecture.LectureViewModel
import com.pulse.presentation.lecture.PlayerProvider
import com.pulse.presentation.library.LibraryViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    
    // Core, Utils & Local
    single<ILogger> { AndroidLogger() }
    single<IFileTypeDetector> { DefaultFileTypeDetector() }
    single { SettingsManager(androidContext()) }
    single { FileStorageManager(androidContext()) }
    
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
    
    // Services
    single { DriveAuthManager(androidContext()) }
    single<IDriveAuthManager> { get<DriveAuthManager>() }
    
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    single<IDriveService> { DriveService(get(), get()) }
    
    // UseCases
    single { SyncLecturesUseCase(get(), get(), get()) }
    single { GetLectureStreamUrlUseCase(get(), get()) }
    
    // Repositories
    single<INoteRepository> { NoteRepository(get()) }
    single { LectureRepository(get(), get(), get(), get(), get(), get()) }
    
    // Player
    single { PlayerProvider(androidContext(), get()) }
    
    // ViewModels
    viewModel { LibraryViewModel(get(), get()) }
    
    viewModel { (lectureId: String) -> 
        LectureViewModel(lectureId, get(), get(), get(), get(), get()) 
    }
}
