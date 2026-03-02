package com.pulse.data.services.btr

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import com.pulse.core.domain.util.ILogger
import androidx.work.WorkerParameters
import com.pulse.data.repository.LectureRepository
import com.pulse.core.domain.util.onSuccess
import com.pulse.core.domain.util.onError

class BtrSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: LectureRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("BtrSyncWorker", "Starting background sync...")
        var workResult = Result.failure()
        
        repository.sync()
            .onSuccess {
                Log.d("BtrSyncWorker", "Background sync completed successfully.")
                workResult = Result.success()
            }
            .onError { e, message ->
                if (e is PulseAuthException) {
                    Log.w("BtrSyncWorker", "Sync skipped due to auth issue: $message")
                    workResult = Result.retry()
                } else {
                    Log.e("BtrSyncWorker", "Sync failed with error: ", e)
                    workResult = Result.failure()
                }
            }
            
        return workResult
    }
}
