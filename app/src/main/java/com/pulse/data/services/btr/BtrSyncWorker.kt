package com.pulse.data.services.btr

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import com.pulse.core.domain.util.ILogger
import androidx.work.WorkerParameters
import com.pulse.data.repository.LectureRepository

class BtrSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: LectureRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("BtrSyncWorker", "Starting background sync...")
        return try {
            repository.sync()
            Log.d("BtrSyncWorker", "Background sync completed successfully.")
            Result.success()
        } catch (e: PulseAuthException) {
            Log.w("BtrSyncWorker", "Sync skipped due to auth issue: ${e.message}")
            Result.retry()
        } catch (e: Exception) {
            Log.e("BtrSyncWorker", "Sync failed with error: ", e)
            Result.failure()
        }
    }
}
