package com.pulse.domain.usecase

import com.pulse.core.data.db.Lecture
import com.pulse.data.services.btr.BtrFile
import com.pulse.data.local.FileStorageManager
import com.pulse.core.domain.util.IFileTypeDetector
import com.pulse.core.domain.util.ILogger

class SyncLecturesUseCase(
    private val fileTypeDetector: IFileTypeDetector,
    private val logger: ILogger,
    private val fileStorage: FileStorageManager
) {
    operator fun invoke(files: List<BtrFile>): List<Lecture> {
        val map = mutableMapOf<String, Pair<BtrFile?, BtrFile?>>()
        
        logger.d("LectureSync", "Grouping ${files.size} files from Drive")

        for (file in files) {
            val isVideo = fileTypeDetector.isVideo(file.mimeType, file.name)
            val isPdf = fileTypeDetector.isPdf(file.mimeType, file.name)
            
            if (!isVideo && !isPdf) continue

            val baseFileName = if (file.name.contains(".")) file.name.substringBeforeLast(".") else file.name
            val isGenericName = fileTypeDetector.isGenericName(file.name)
            
            val lectureName = if (isGenericName && file.parentName != null && file.parentName != "Drive") {
                file.parentName
            } else {
                baseFileName
            }
            
            val groupingKey = if (file.parentName != null && file.parentName != "Drive") {
                "${file.parentName}_$lectureName"
            } else {
                lectureName
            }

            val pair = map[groupingKey] ?: Pair(null, null)
            if (isVideo) {
                if (pair.first != null) {
                    val uniqueKey = "${groupingKey}_${file.id.take(4)}"
                    map[uniqueKey] = Pair(file, null)
                } else {
                    map[groupingKey] = pair.copy(first = file)
                }
            } else {
                if (pair.second != null) {
                    val uniqueKey = "${groupingKey}_${file.id.take(4)}"
                    map[uniqueKey] = Pair(null, file)
                } else {
                    map[groupingKey] = pair.copy(second = file)
                }
            }
        }

        val result = map.entries.mapNotNull { (key, pair) ->
            val video = pair.first
            val pdf = pair.second
            
            if (video != null || pdf != null) {
                var displayName = if (key.contains("_")) {
                    val parts = key.split("_")
                    if (parts.size >= 2 && parts[0] == parts[1]) {
                        parts[0] 
                    } else if (parts.size >= 2 && (parts[1].length == 4 && parts[1].all { it.isLetterOrDigit() })) {
                        key.substringBeforeLast("_")
                    } else {
                        key.replace("_", " - ")
                    }
                } else {
                    key
                }

                displayName = displayName.replace("[Medicalstudyzone.com]", "", ignoreCase = true).trim()

                logger.d("LectureSync", "Created Lecture: $displayName")
                Lecture(
                    id = video?.id ?: pdf?.id ?: key,
                    name = displayName,
                    videoId = video?.id,
                    pdfId = pdf?.id,
                    pdfLocalPath = fileStorage.pdfDir.absolutePath + "/${key.filter { it.isLetterOrDigit() }}.pdf",
                    videoLocalPath = null,
                    isLocal = false
                )
            } else null
        }

        logger.d("LectureSync", "Sync summary: ${result.size} lectures found")
        return result
    }
}
