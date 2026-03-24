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
            val syncType = inputData.getString("SYNC_TYPE") ?: "BOTH"
            val isFullSync = inputData.getBoolean("FULL_SYNC", false)
            val lastSync = if (isFullSync) 0L else settingsManager.lastSyncTimeFlow.first()
            
            var pushedLectures = 0
            var pushedNotes = 0
            var pushedVisuals = 0
            var pulledLectures = 0
            var pulledNotes = 0
            var pulledVisuals = 0

            // --- PUSH: Local changes -> Firestore ---
            if (syncType == "BOTH" || syncType == "PUSH") {
                val modifiedLectures = if (isFullSync) lectureDao.getAllLecturesAsList() else lectureDao.getModifiedSince(lastSync)
                val modifiedNotes = if (isFullSync) noteDao.getAllNotesAsList() else noteDao.getModifiedSince(lastSync)
                val modifiedVisuals = if (isFullSync) noteVisualDao.getAllVisuals() else noteVisualDao.getModifiedSince(lastSync)

                val filteredLectures = modifiedLectures.filter { !it.isLocal }

                syncManager.pushLectures(filteredLectures)
                syncManager.pushNotes(modifiedNotes)
                syncManager.pushNoteVisuals(modifiedVisuals)

                pushedLectures = filteredLectures.size
                pushedNotes = modifiedNotes.size
                pushedVisuals = modifiedVisuals.size
            }

            // --- PULL: Firestore -> Local (with HLC conflict resolution) ---
            if (syncType == "BOTH" || syncType == "PULL") {
                val remoteLectures = syncManager.pullLectures(lastSync)
                if (remoteLectures.isNotEmpty()) {
                    val localMap = lectureDao.getAllLecturesAsList().associateBy { it.id }
                    val toInsert = remoteLectures.filter { remote ->
                        val local = localMap[remote.id]
                        local == null || HlcGenerator.compare(remote.hlcTimestamp, local.hlcTimestamp) > 0 || isFullSync
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
                    pulledLectures = remoteLectures.size
                }

                val remoteNotes = syncManager.pullNotes(lastSync)
                if (remoteNotes.isNotEmpty()) {
                    val localNoteMap = noteDao.getAllNotesAsList().associateBy { it.id }
                    val toInsert = remoteNotes.filter { remote ->
                        val local = localNoteMap[remote.id]
                        local == null || HlcGenerator.compare(remote.hlcTimestamp, local.hlcTimestamp) > 0 || isFullSync
                    }
                    if (toInsert.isNotEmpty()) noteDao.insertAll(toInsert)
                    pulledNotes = remoteNotes.size
                }

                val remoteVisuals = syncManager.pullNoteVisuals(lastSync)
                if (remoteVisuals.isNotEmpty()) {
                    // To avoid loading the entire DB for visuals, we just insert. Room's onConflictStrategy will handle it
                    // Or we can query the specific IDs if needed, but assuming a reasonable amount for now.
                    noteVisualDao.insertAll(remoteVisuals)
                    pulledVisuals = remoteVisuals.size
                }
            }

            if (syncType == "BOTH") {
                settingsManager.saveLastSyncTime(System.currentTimeMillis())
            }
            
            Log.d("FirestoreSyncWorker", "Sync OK. Push: ${pushedLectures}L/${pushedNotes}N/${pushedVisuals}V. Pull: ${pulledLectures}L/${pulledNotes}N/${pulledVisuals}V")
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

        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun enqueueBackup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString("SYNC_TYPE", "PUSH")
                .putBoolean("FULL_SYNC", true)
                .build()

            val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_backup",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun enqueueRestore(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString("SYNC_TYPE", "PULL")
                .putBoolean("FULL_SYNC", true)
                .build()

            val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_restore",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
