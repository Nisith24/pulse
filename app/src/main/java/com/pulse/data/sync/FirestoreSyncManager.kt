package com.pulse.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.pulse.core.data.db.Lecture
import com.pulse.core.data.db.Note
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val lectureDao: com.pulse.data.db.LectureDao,
    private val noteDao: com.pulse.data.db.NoteDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val db: FirebaseFirestore = Firebase.firestore
    private val userId: String? get() = auth.currentUser?.uid

    // --- DIRECT PUSH (fire-and-forget, no WorkManager) ---

    fun pushSingleLecture(lecture: Lecture) {
        val uid = userId ?: return
        if (lecture.isLocal) return
        val ref = db.collection("users").document(uid).collection("lectures")
        ref.document(lecture.id).set(lectureToMap(lecture), SetOptions.merge())
            .addOnSuccessListener { Log.d("FirestoreSync", "Pushed: ${lecture.name} pos=${lecture.lastPosition}") }
            .addOnFailureListener { Log.e("FirestoreSync", "Push failed: ${it.message}") }
    }

    // --- BATCH PUSH: Room -> Firestore ---

    suspend fun pushLectures(lectures: List<Lecture>) {
        val uid = userId ?: return
        if (lectures.isEmpty()) return
        val ref = db.collection("users").document(uid).collection("lectures")
        try {
            lectures.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.set(ref.document(it.id), lectureToMap(it), SetOptions.merge()) }
                batch.commit().await()
            }
            Log.d("FirestoreSync", "Batch pushed ${lectures.size} lectures")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Batch push lectures error", e)
        }
    }

    suspend fun pushNotes(notes: List<Note>) {
        val uid = userId ?: return
        if (notes.isEmpty()) return
        val ref = db.collection("users").document(uid).collection("notes")
        try {
            notes.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.set(ref.document(it.id.toString()), noteToMap(it), SetOptions.merge()) }
                batch.commit().await()
            }
            Log.d("FirestoreSync", "Batch pushed ${notes.size} notes")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Batch push notes error", e)
        }
    }

    // --- PULL: Firestore -> Room ---

    suspend fun pullLectures(lastSyncTime: Long = 0): List<Lecture> {
        val uid = userId ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid).collection("lectures")
                .whereGreaterThan("updatedAt", lastSyncTime)
                .get().await()
            snapshot.documents.mapNotNull { doc -> mapToLecture(doc.data ?: return@mapNotNull null) }
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Pull lectures error", e)
            emptyList()
        }
    }

    suspend fun pullNotes(lastSyncTime: Long = 0): List<Note> {
        val uid = userId ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid).collection("notes")
                .whereGreaterThan("createdAt", lastSyncTime)
                .get().await()
            snapshot.documents.mapNotNull { doc -> mapToNote(doc.data ?: return@mapNotNull null) }
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Pull notes error", e)
            emptyList()
        }
    }

    // --- FULL PULL (for app launch / BTR refresh) ---

    suspend fun pullAndMergeAll() {
        val uid = userId ?: return
        try {
            val remoteLectures = db.collection("users").document(uid).collection("lectures")
                .get().await()
                .documents.mapNotNull { doc -> mapToLecture(doc.data ?: return@mapNotNull null) }

            if (remoteLectures.isNotEmpty()) {
                val localMap = lectureDao.getAllLecturesAsList().associateBy { it.id }
                val toInsert = remoteLectures.filter { remote ->
                    val local = localMap[remote.id]
                    local == null || com.pulse.core.domain.util.HlcGenerator.compare(remote.hlcTimestamp, local.hlcTimestamp) > 0
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
                if (toInsert.isNotEmpty()) {
                    lectureDao.insertAll(toInsert)
                    Log.d("FirestoreSync", "Pull merged ${toInsert.size} lectures")
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSync", "pullAndMergeAll error", e)
        }
    }

    // --- Serialization ---

    private fun lectureToMap(l: Lecture) = mapOf(
        "id" to l.id, "name" to l.name, "videoId" to l.videoId, 
        "lastPosition" to l.lastPosition, "videoDuration" to l.videoDuration,
        "isFavorite" to l.isFavorite,
        "hlcTimestamp" to l.hlcTimestamp,
        "updatedAt" to l.updatedAt
    )

    private fun noteToMap(n: Note) = mapOf(
        "id" to n.id, "lectureId" to n.lectureId, "timestamp" to n.timestamp,
        "text" to n.text, "createdAt" to n.createdAt, "hlcTimestamp" to n.hlcTimestamp,
        "isDeleted" to n.isDeleted
    )

    private fun mapToLecture(m: Map<String, Any>): Lecture = Lecture(
        id = m["id"] as? String ?: "", name = m["name"] as? String ?: "",
        videoId = m["videoId"] as? String, pdfId = null,
        pdfLocalPath = "", videoLocalPath = null,
        isPdfDownloaded = false, isLocal = false,
        lastPosition = (m["lastPosition"] as? Number)?.toLong() ?: 0,
        videoDuration = (m["videoDuration"] as? Number)?.toLong() ?: 0,
        speed = 1f,
        isFavorite = m["isFavorite"] as? Boolean ?: false,
        pdfPageCount = 0,
        lastPdfPage = 0,
        pdfIsHorizontal = false,
        hlcTimestamp = m["hlcTimestamp"] as? String ?: "",
        isDeleted = m["isDeleted"] as? Boolean ?: false,
        updatedAt = (m["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    )

    private fun mapToNote(m: Map<String, Any>): Note = Note(
        id = (m["id"] as? Number)?.toLong() ?: 0,
        lectureId = m["lectureId"] as? String ?: "",
        timestamp = (m["timestamp"] as? Number)?.toLong() ?: 0,
        text = m["text"] as? String ?: "",
        createdAt = (m["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        hlcTimestamp = m["hlcTimestamp"] as? String ?: "",
        isDeleted = m["isDeleted"] as? Boolean ?: false
    )
}
