package com.pulse

import android.app.Application
import com.pulse.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import androidx.work.*

import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.android.ext.android.getKoin
import org.koin.core.context.GlobalContext
import android.content.ComponentCallbacks2

class PulseApp : Application(), Configuration.Provider {
    
    override val workManagerConfiguration: Configuration
        get() {
            // Ensure Koin is started before WorkManager tries to get the factory
            if (GlobalContext.getOrNull() == null) {
                initKoin()
            }
            return Configuration.Builder()
                .setWorkerFactory(getKoin().get<KoinWorkerFactory>())
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() == null) {
            initKoin()
        }
        
        // Initialize notifications
        com.pulse.core.domain.util.NotificationHelper.createNotificationChannel(this)
        
        // Start periodic services
        com.pulse.data.sync.FirestoreSyncWorker.enqueuePeriodicSync(this)
        com.pulse.data.worker.QuoteWorker.enqueuePeriodicQuote(this)
    }

    private fun initKoin() {
        startKoin {
            androidContext(this@PulseApp)
            workManagerFactory()
            modules(appModule)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            GlobalContext.getOrNull()?.let { koin ->
                try {
                    val playerProvider = koin.get<com.pulse.presentation.lecture.PlayerProvider>()
                    if (!playerProvider.player.playWhenReady) {
                        playerProvider.player.stop()
                        playerProvider.player.clearMediaItems()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PulseApp", "Trim memory player clear failed", e)
                }
            }
        }
    }
}
