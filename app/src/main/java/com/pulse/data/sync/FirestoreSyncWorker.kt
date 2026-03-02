package com.pulse.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.pulse.core.domain.util.HlcGenerator
import com.pulse.data.db.LectureDao
import com.pulse.data.db.NoteDao
import com.pulse.data.db.NoteVisualDao
import com.pulse.data.local.SettingsManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class FirestoreSyncWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val lectureDao: LectureDao,
    private val noteDao: NoteDao,
    private val noteVisualDao: NoteVisualDao,
    private val settingsManager: SettingsManager,
    private val syncManager: FirestoreSyncManager,
    private val hlcGenerator: HlcGenerator
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val lastSync = settingsManager.lastSyncTimeFlow.first()
            
            // --- PUSH: Local changes -> Firestore ---
            val modifiedLectures = lectureDao.getModifiedSince(lastSync)
            val modifiedNotes = noteDao.getModifiedSince(lastSync)

            val filteredLectures = modifiedLectures.filter { !it.isLocal }
            
            syncManager.pushLectures(filteredLectures)
            syncManager.pushNotes(modifiedNotes)

            // --- PULL: Firestore -> Local (with HLC conflict resolution) ---
            val remoteLectures = syncManager.pullLectures(lastSync)
            if (remoteLectures.isNotEmpty()) {
                val localMap = lectureDao.getAllLecturesAsList().associateBy { it.id }
                val toInsert = remoteLectures.filter { remote ->
                    val local = localMap[remote.id]
                    local == null || HlcGenerator.compare(remote.hlcTimestamp, local.hlcTimestamp) > 0
                }.map { remote ->
                    val local = localMap[remote.id]
                    if (local != null) {
                        remote.copy(
                            pdfLocalPath = local.pdfLocalPath,
                            videoLocalPath = local.videoLocalPath,
                            isPdfDownloaded = local.isPdfDownloaded,
                            pdfId = local.pdfId,
                            speed = local.speed,
                            pdfPageCount = local.pdfPageCount,
                            lastPdfPage = local.lastPdfPage,
                            pdfIsHorizontal = local.pdfIsHorizontal
                        )
                    } else remote
                }
                if (toInsert.isNotEmpty()) lectureDao.insertAll(toInsert)
            }

            val remoteNotes = syncManager.pullNotes(lastSync)
            if (remoteNotes.isNotEmpty()) {
                val localNoteMap = noteDao.getAllNotesAsList().associateBy { it.id }
                val toInsert = remoteNotes.filter { remote ->
                    val local = localNoteMap[remote.id]
                    local == null || HlcGenerator.compare(remote.hlcTimestamp, local.hlcTimestamp) > 0
                }
                if (toInsert.isNotEmpty()) noteDao.insertAll(toInsert)
            }

            settingsManager.saveLastSyncTime(System.currentTimeMillis())
            
            Log.d("FirestoreSyncWorker", "Sync OK. Push: ${filteredLectures.size}L/${modifiedNotes.size}N. Pull: ${remoteLectures.size}L/${remoteNotes.size}N")
            Result.success()
        } catch (e: Exception) {
            Log.e("FirestoreSyncWorker", "Sync failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "firestore_sync"

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<FirestoreSyncWorker>(
                60, TimeUnit.MINUTES
            ).setConstraints(constraints)
             .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
             .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueDebouncedSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
