package com.pulse.data.services.btr

import com.pulse.core.domain.util.ILogger
import com.pulse.core.domain.util.Constants
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

            // Google Drive API requires either a Bearer token OR an API key — even for public folders.
            // When the user is not signed in, append the API key to authenticate the request.
            val apiKeyParam = if (accessToken.isEmpty()) "&key=${Constants.GOOGLE_API_KEY}" else ""
            val url = "https://www.googleapis.com/drive/v3/files?q='${currentFolderId}'+in+parents+and+trashed=false&fields=files(id,name,mimeType,size)&pageSize=1000&supportsAllDrives=true&includeItemsFromAllDrives=true$apiKeyParam"
            val request = Request.Builder()
                .url(url)
                .apply { if (accessToken.isNotEmpty()) addHeader("Authorization", "Bearer $accessToken") }
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

    override suspend fun listSubfolders(
        folderId: String,
        accessToken: String
    ): List<BtrFile> = withContext(Dispatchers.IO) {
        val apiKeyParam = if (accessToken.isEmpty()) "&key=${Constants.GOOGLE_API_KEY}" else ""
        val url = "https://www.googleapis.com/drive/v3/files?q='${folderId}'+in+parents+and+mimeType='application/vnd.google-apps.folder'+and+trashed=false&fields=files(id,name,mimeType)&pageSize=100&supportsAllDrives=true&includeItemsFromAllDrives=true$apiKeyParam"
        val request = Request.Builder()
            .url(url)
            .apply { if (accessToken.isNotEmpty()) addHeader("Authorization", "Bearer $accessToken") }
            .addHeader("User-Agent", "Pulse/1.0")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    parseBtrFiles(body)
                } else {
                    logger.e("BtrService", "listSubfolders Error [${response.code}]: $body")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.e("BtrService", "listSubfolders exception", e)
            emptyList()
        }
    }

    override fun streamUrl(fileId: String): String =
        "https://www.googleapis.com/drive/v3/files/$fileId?alt=media&supportsAllDrives=true"

    override suspend fun downloadFile(fileId: String, accessToken: String): ByteArray = withContext(Dispatchers.IO) {
        val apiKeyParam = if (accessToken.isEmpty()) "&key=${Constants.GOOGLE_API_KEY}" else ""
        val url = streamUrl(fileId) + apiKeyParam
        val request = Request.Builder()
            .url(url)
            .apply { if (accessToken.isNotEmpty()) addHeader("Authorization", "Bearer $accessToken") }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed: ${response.code}")
            val body = response.body ?: error("Empty response body from Drive API")
            body.bytes()
        }
    }

    override suspend fun downloadRange(fileId: String, accessToken: String, start: Long, end: Long): ByteArray = withContext(Dispatchers.IO) {
        val apiKeyParam = if (accessToken.isEmpty()) "&key=${Constants.GOOGLE_API_KEY}" else ""
        val url = streamUrl(fileId) + apiKeyParam
        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$start-$end")
            .apply { if (accessToken.isNotEmpty()) addHeader("Authorization", "Bearer $accessToken") }
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code != 206 && response.code != 200) error("Range download failed: ${response.code}")
            val body = response.body ?: error("Empty response body from Drive API")
            body.bytes()
        }
    }

    override suspend fun downloadToStream(fileId: String, outputStream: OutputStream, accessToken: String, onProgress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val apiKeyParam = if (accessToken.isEmpty()) "&key=${Constants.GOOGLE_API_KEY}" else ""
        val url = streamUrl(fileId) + apiKeyParam
        val request = Request.Builder()
            .url(url)
            .apply { if (accessToken.isNotEmpty()) addHeader("Authorization", "Bearer $accessToken") }
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
                val buffer = ByteArray(131072) // 128KB buffer for faster throughput
                var totalRead = 0L
                var read: Int
                var lastUpdate = System.currentTimeMillis()
                while (input.read(buffer).also { read = it } != -1) {
                    kotlinx.coroutines.yield()
                    output.write(buffer, 0, read)
                    totalRead += read
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 200) { // smoother UI progress at 200ms
                        onProgress(totalRead, contentLen)
                        lastUpdate = now
                    }
                }
                onProgress(totalRead, contentLen)
            }
        }
    }
}
