package com.pulse.data.drive

import com.pulse.data.drive.DriveAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

import com.pulse.domain.service.IDriveService
import com.pulse.domain.service.IDriveAuthManager

class DriveService(
    private val client: OkHttpClient,
    private val authManager: IDriveAuthManager
) : IDriveService {
    override suspend fun listFolder(
        folderId: String,
        accessToken: String,
        recursive: Boolean
    ): List<DriveFile> = withContext(Dispatchers.IO) {
        val rootList = mutableListOf<DriveFile>()
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
                                val files = parseDriveFiles(retryBody)
                                handleFiles(files, rootList, foldersToProcess, currentFolderName, recursive)
                            }
                        }
                    } else if (response.isSuccessful) {
                        val files = parseDriveFiles(body)
                        handleFiles(files, rootList, foldersToProcess, currentFolderName, recursive)
                    } else {
                        android.util.Log.e("DriveService", "Drive error: ${response.code} for folder $currentFolderId")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DriveService", "Failed to list folder $currentFolderId: ${e.message}")
            }
        }
        rootList
    }

    private fun handleFiles(
        files: List<DriveFile>,
        rootList: MutableList<DriveFile>,
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

    private fun parseDriveFiles(json: String): List<DriveFile> {
        val rootList = mutableListOf<DriveFile>()
        try {
            val jsonObject = JSONObject(json)
            val filesArray = jsonObject.optJSONArray("files")
            if (filesArray != null) {
                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.getJSONObject(i)
                    rootList.add(
                        DriveFile(
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
