package com.pulse.data.services.btr

import com.pulse.core.domain.util.ILogger
import com.pulse.domain.services.btr.IBtrAuthManager
import com.pulse.domain.services.btr.IBtrService
import java.io.File
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject



class GoogleDriveBtrService(
    private val client: OkHttpClient,
    private val authManager: IBtrAuthManager,
    private val logger: ILogger
) : IBtrService {
    override suspend fun listFolder(
        folderId: String,
        accessToken: String,
        recursive: Boolean
    ): List<BtrFile> = withContext(Dispatchers.IO) {
        val rootList = mutableListOf<BtrFile>()
        val foldersToProcess = mutableListOf(Pair(folderId, "Drive"))
        val processedFolders = mutableSetOf<String>()

        while (foldersToProcess.isNotEmpty()) {
            val (currentFolderId, currentFolderName) = foldersToProcess.removeAt(0)
            if (processedFolders.contains(currentFolderId)) continue
            processedFolders.add(currentFolderId)

            val url = "https://www.googleapis.com/drive/v3/files?q='${currentFolderId}'+in+parents+and+trashed=false&fields=files(id,name,mimeType,size)&pageSize=1000"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("User-Agent", "Pulse/1.0")
                .build()

            try {
                logger.d("BtrService", "Listing folder: $currentFolderName ($currentFolderId) at $url")
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    logger.d("BtrService", "Response [${response.code}] for $currentFolderName: ${if (body.length > 200) body.take(200) + "..." else body}")
                    
                    if (response.isSuccessful) {
                        val files = parseBtrFiles(body)
                        logger.d("BtrService", "Parsed ${files.size} files from $currentFolderName")
                        handleFiles(files, rootList, foldersToProcess, currentFolderName, recursive)
                    } else {
                        logger.e("BtrService", "API Error [${response.code}]: $body")
                    }
                }
            } catch (e: Exception) {
                logger.e("BtrService", "Network/API exception for $currentFolderId", e)
            }
        }
        rootList
    }

    private fun handleFiles(files: List<BtrFile>, rootList: MutableList<BtrFile>, foldersToProcess: MutableList<Pair<String, String>>, parentName: String, recursive: Boolean) {
        for (file in files) {
            if (file.mimeType == "application/vnd.google-apps.folder") {
                if (recursive) foldersToProcess.add(Pair(file.id, file.name))
            } else {
                rootList.add(file.copy(parentName = parentName))
            }
        }
    }

    override fun streamUrl(fileId: String): String =
        "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"

    override suspend fun downloadFile(fileId: String, accessToken: String): ByteArray = withContext(Dispatchers.IO) {
        val url = streamUrl(fileId)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed: ${response.code}")
            response.body!!.bytes()
        }
    }

    override suspend fun downloadToStream(fileId: String, outputStream: OutputStream, accessToken: String, onProgress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val url = streamUrl(fileId)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Fetch failed: ${response.code}")
            streamBody(response, outputStream, onProgress)
        }
    }

    private fun parseBtrFiles(json: String): List<BtrFile> {
        val list = mutableListOf<BtrFile>()
        try {
            val arr = JSONObject(json).optJSONArray("files") ?: return list
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(BtrFile(id = o.getString("id"), name = o.getString("name"), mimeType = o.getString("mimeType"), size = if (o.has("size")) o.getLong("size") else null))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    private suspend fun streamBody(response: Response, outputStream: OutputStream, onProgress: (Long, Long) -> Unit) {
        val body = response.body ?: return
        val contentLen = body.contentLength()
        body.byteStream().use { input ->
            outputStream.use { output ->
                val buffer = ByteArray(65536)
                var totalRead = 0L
                var read: Int
                var lastUpdate = System.currentTimeMillis()
                while (input.read(buffer).also { read = it } != -1) {
                    kotlinx.coroutines.yield()
                    output.write(buffer, 0, read)
                    totalRead += read
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 300) {
                        onProgress(totalRead, contentLen)
                        lastUpdate = now
                    }
                }
                onProgress(totalRead, contentLen)
            }
        }
    }
}
