package com.pulse

import android.app.Application
import com.pulse.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@PulseApp)
            modules(appModule)
        }
    }
}
