package com.pulse.di

import androidx.room.Room
import com.pulse.data.db.AppDatabase
import com.pulse.data.services.btr.FirebasePulseAuthManager
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
        ).addMigrations(AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14)
         .fallbackToDestructiveMigration()
         .build()
    }
    
    single { get<AppDatabase>().lectureDao() }
    single { get<AppDatabase>().noteDao() }
    single { get<AppDatabase>().noteVisualDao() }
    single { get<AppDatabase>().customListDao() }
    
    // HLC Generator for CRDT
    single { 
        val androidId = android.provider.Settings.Secure.getString(
            androidContext().contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: java.util.UUID.randomUUID().toString()
        com.pulse.core.domain.util.HlcGenerator(androidId) 
    }
    
    // Services
    single { FirebasePulseAuthManager(androidContext()) }
    single<IBtrAuthManager> { get<FirebasePulseAuthManager>() }
    
    single {
        val retryInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            var lastException: Exception? = null
            for (attempt in 0..2) {
                try {
                    val response = chain.proceed(request)
                    if (response.isSuccessful || response.code !in listOf(502, 503, 504)) {
                        return@Interceptor response
                    }
                    response.close()
                } catch (e: java.io.IOException) {
                    lastException = e
                }
                if (attempt < 2) {
                    try { Thread.sleep((300L * (attempt + 1))) } catch (ignored: InterruptedException) {}
                }
            }
            throw lastException ?: java.io.IOException("Retry exhausted for ${request.url}")
        }

        OkHttpClient.Builder()
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
            .addInterceptor(retryInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
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
    single { LectureRepository(get(), get(), get(), get(), get(), get(), get(), androidContext(), get(), get()) }
    
    // Player — MUST be singleton (SimpleCache uses exclusive DB lock)
    single { PlayerProvider(androidContext(), get()) }
    
    // ViewModels
    viewModel { com.pulse.presentation.theme.ThemeViewModel(get()) }
    viewModel { LibraryViewModel(get(), get(), get()) }
    viewModel { DownloadsViewModel(get()) }
    
    viewModel { (lectureId: String) -> 
        LectureViewModel(lectureId, get(), get(), get(), get(), get(), get(), get()) 
    }
    viewModel { com.pulse.presentation.subjects.SubjectDetailViewModel(get(), get()) }
    viewModel { com.pulse.presentation.prepladderrr.PrepladderRRViewModel(get()) }
    viewModel { com.pulse.presentation.customlist.CustomListViewModel(get()) }
    
    // Workers
    worker { com.pulse.data.services.btr.BtrSyncWorker(get(), get(), get()) }
    worker { com.pulse.data.sync.FirestoreSyncWorker(get(), get(), get(), get(), get(), get(), get(), get()) }

    // Firestore Sync
    single { com.pulse.data.sync.FirestoreSyncManager(get(), get()) }
}
