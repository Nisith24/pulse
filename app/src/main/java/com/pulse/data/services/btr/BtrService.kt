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

            val url = "https://www.googleapis.com/drive/v3/files" +
                    "?q='${currentFolderId}'+in+parents+and+trashed=false" +
                    "&fields=files(id,name,mimeType,size)" +
                    "&pageSize=1000"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("User-Agent", "Pulse/1.0")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.code == 401) {
                        // Attempt token refresh once
                        val newToken = authManager.getToken()
                        val retryRequest = request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                        client.newCall(retryRequest).execute().use { retryResponse ->
                            if (retryResponse.isSuccessful) {
                                val retryBody = retryResponse.body?.string() ?: ""
                                val files = parseBtrFiles(retryBody)
                                handleFiles(files, rootList, foldersToProcess, currentFolderName, recursive)
                            }
                        }
                    } else if (response.isSuccessful) {
                        val files = parseBtrFiles(body)
                        handleFiles(files, rootList, foldersToProcess, currentFolderName, recursive)
                    } else {
                        logger.e("BtrService", "Drive API error: ${response.code} for folder $currentFolderId")
                    }
                }
            } catch (e: Exception) {
                logger.e("BtrService", "Failed to list folder $currentFolderId", e)
            }
        }
        logger.d("BtrService", "Completed listing ${rootList.size} files from $folderId")
        rootList
    }

    private fun handleFiles(
        files: List<BtrFile>,
        rootList: MutableList<BtrFile>,
        foldersToProcess: MutableList<Pair<String, String>>,
        parentName: String,
        recursive: Boolean
    ) {
        for (file in files) {
            if (file.mimeType == "application/vnd.google-apps.folder") {
                if (recursive) {
                    foldersToProcess.add(Pair(file.id, file.name))
                }
            } else {
                rootList.add(file.copy(parentName = parentName))
            }
        }
    }

    override fun streamUrl(fileId: String): String =
        "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"

    override suspend fun downloadFile(fileId: String, accessToken: String): ByteArray = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("User-Agent", "Pulse/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401) {
                val newToken = authManager.getToken()
                val retryRequest = request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                client.newCall(retryRequest).execute().use { retryResponse ->
                    if (!retryResponse.isSuccessful) error("Download failed after refresh: ${retryResponse.code}")
                    retryResponse.body!!.bytes()
                }
            } else if (!response.isSuccessful) {
                error("Download failed: ${response.code}")
            } else {
                response.body!!.bytes()
            }
        }
    }

    override suspend fun downloadToStream(
        fileId: String,
        outputStream: OutputStream,
        accessToken: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val downloadClient = client.newBuilder()
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
            
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("User-Agent", "Pulse/1.0")
            .build()
            
        downloadClient.newCall(request).execute().use { response ->
            if (response.code == 401) {
                val newToken = authManager.getToken()
                val retryReq = request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                downloadClient.newCall(retryReq).execute().use { retryResp ->
                    if (!retryResp.isSuccessful) error("Failed to download after refresh: ${retryResp.code}")
                    streamBody(retryResp, outputStream, onProgress)
                }
            } else if (!response.isSuccessful) {
                error("Failed to download: ${response.code}")
            } else {
                streamBody(response, outputStream, onProgress)
            }
        }
    }

    private suspend fun streamBody(response: okhttp3.Response, outputStream: OutputStream, onProgress: (Long, Long) -> Unit) {
        val body = response.body ?: error("No response body")
        val contentLen = body.contentLength()
        body.byteStream().use { input ->
            outputStream.use { output ->
                val buffer = ByteArray(64 * 1024) // Increased to 64KB for high-speed streaming
                var totalRead = 0L
                var read: Int
                var lastUpdate = System.currentTimeMillis()
                
                while (input.read(buffer).also { read = it } != -1) {
                    kotlinx.coroutines.yield() // Support cancellation
                    output.write(buffer, 0, read)
                    totalRead += read
                    val now = System.currentTimeMillis()
                    if (now - lastUpdate > 250 || totalRead == contentLen) {
                        onProgress(totalRead, contentLen)
                        lastUpdate = now
                    }
                }
                onProgress(totalRead, contentLen)
            }
        }
    }
    private fun parseBtrFiles(json: String): List<BtrFile> {
        val rootList = mutableListOf<BtrFile>()
        try {
            val jsonObject = JSONObject(json)
            val filesArray = jsonObject.optJSONArray("files")
            if (filesArray != null) {
                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.getJSONObject(i)
                    rootList.add(
                        BtrFile(
                            id = fileObj.getString("id"),
                            name = fileObj.getString("name"),
                            mimeType = fileObj.getString("mimeType"),
                            size = if (fileObj.has("size")) fileObj.getLong("size") else null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rootList
    }
}
