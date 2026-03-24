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

            var pushedLecturesCount = 0
            var pushedNotesCount = 0
            var pushedVisualsCount = 0
            var pulledLecturesCount = 0
            var pulledNotesCount = 0
            var pulledVisualsCount = 0
            
            // --- PUSH: Local changes -> Firestore ---
            if (syncType == "PUSH" || syncType == "BOTH") {
                val modifiedLectures = lectureDao.getModifiedSince(lastSync)
                val modifiedNotes = noteDao.getModifiedSince(lastSync)
                val modifiedVisuals = noteVisualDao.getModifiedSince(lastSync)

                val filteredLectures = modifiedLectures.filter { !it.isLocal }

                syncManager.pushLectures(filteredLectures)
                syncManager.pushNotes(modifiedNotes)
                syncManager.pushNoteVisuals(modifiedVisuals)

                pushedLecturesCount = filteredLectures.size
                pushedNotesCount = modifiedNotes.size
                pushedVisualsCount = modifiedVisuals.size
            }

            // --- PULL: Firestore -> Local (with HLC conflict resolution) ---
            if (syncType == "PULL" || syncType == "BOTH") {
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
                pulledLecturesCount = remoteLectures.size

                val remoteNotes = syncManager.pullNotes(lastSync)
                if (remoteNotes.isNotEmpty()) {
                    val localNoteMap = noteDao.getAllNotesAsList().associateBy { it.id }
                    val toInsert = remoteNotes.filter { remote ->
                        val local = localNoteMap[remote.id]
                        local == null || HlcGenerator.compare(remote.hlcTimestamp, local.hlcTimestamp) > 0
                    }
                    if (toInsert.isNotEmpty()) noteDao.insertAll(toInsert)
                }
                pulledNotesCount = remoteNotes.size

                val remoteVisuals = syncManager.pullNoteVisuals(lastSync)
                if (remoteVisuals.isNotEmpty()) {
                    // To avoid loading the entire DB for visuals, we just insert. Room's onConflictStrategy will handle it
                    // Or we can query the specific IDs if needed, but assuming a reasonable amount for now.
                    noteVisualDao.insertAll(remoteVisuals)
                }
                pulledVisualsCount = remoteVisuals.size
            }

            // Only advance the global sync cursor if we're doing a full BOTH sync,
            // otherwise a manual PUSH/PULL might skip future updates in the other direction.
            if (syncType == "BOTH" && !isFullSync) {
                settingsManager.saveLastSyncTime(System.currentTimeMillis())
            }
            
            Log.d("FirestoreSyncWorker", "Sync OK ($syncType). Push: ${pushedLecturesCount}L/${pushedNotesCount}N/${pushedVisualsCount}V. Pull: ${pulledLecturesCount}L/${pulledNotesCount}N/${pulledVisualsCount}V")
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

        fun enqueueImmediateSync(context: Context, syncType: String, isFullSync: Boolean): java.util.UUID {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = Data.Builder()
                .putString("SYNC_TYPE", syncType)
                .putBoolean("FULL_SYNC", isFullSync)
                .build()

            val request = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_manual_$syncType",
                ExistingWorkPolicy.REPLACE,
                request
            )

            return request.id
        }
    }
}
