package com.compass.diary.data.repository

import android.content.Context
import com.compass.diary.util.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Drive repository for diary cloud synchronisation.
 *
 * HOW TO WIRE UP (full integration requires):
 * 1. Add google-services.json from Firebase Console (or Google Cloud Console)
 * 2. Enable the Drive API in Google Cloud Console
 * 3. Request Drive scope: "https://www.googleapis.com/auth/drive.appdata"
 * 4. Retrieve OAuth token via GoogleSignIn.getClient(...).silentSignIn()
 * 5. Pass the token to the methods below
 *
 * This class provides the full REST implementation using OkHttp so you have
 * complete control over the network layer without depending on the full
 * google-api-services-drive library.
 */
@Singleton
class GoogleDriveRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) {
    companion object {
        private const val DRIVE_BASE      = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD    = "https://www.googleapis.com/upload/drive/v3"
        private const val APP_FOLDER_NAME = "CompassDiary"
        private const val MIME_JSON       = "application/json"
        private const val MIME_FOLDER     = "application/vnd.google-apps.folder"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ─── FOLDER MANAGEMENT ───────────────────────────────────────

    /**
     * Find or create the "CompassDiary" appDataFolder on Drive.
     * Returns the folder ID.
     */
    suspend fun getOrCreateAppFolder(accessToken: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Search for existing folder
                val searchUrl = "$DRIVE_BASE/files?q=" +
                        "name='$APP_FOLDER_NAME' and mimeType='$MIME_FOLDER' and trashed=false" +
                        "&fields=files(id,name)"
                val searchReq = Request.Builder()
                    .url(searchUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()
                val searchResp = client.newCall(searchReq).execute()
                val searchJson = JSONObject(searchResp.body?.string() ?: "{}")
                val files = searchJson.optJSONArray("files")

                if (files != null && files.length() > 0) {
                    val folderId = files.getJSONObject(0).getString("id")
                    prefs.setDriveFolderId(folderId)
                    return@withContext Result.success(folderId)
                }

                // 2. Create the folder
                val metaBody = JSONObject().apply {
                    put("name", APP_FOLDER_NAME)
                    put("mimeType", MIME_FOLDER)
                }.toString().toRequestBody(MIME_JSON.toMediaType())

                val createReq = Request.Builder()
                    .url("$DRIVE_BASE/files")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(metaBody)
                    .build()
                val createResp = client.newCall(createReq).execute()
                val createJson = JSONObject(createResp.body?.string() ?: "{}")
                val folderId   = createJson.getString("id")
                prefs.setDriveFolderId(folderId)
                Result.success(folderId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── FILE UPLOAD ─────────────────────────────────────────────

    /**
     * Upload (create or update) a diary entry JSON to Drive.
     * @param accessToken  Valid OAuth2 bearer token
     * @param fileName     e.g. "2026-06-07.json"
     * @param content      JSON string to upload
     * @param existingId   If non-null, updates the existing file; otherwise creates
     * @return Drive file ID
     */
    suspend fun uploadFile(
        accessToken: String,
        fileName: String,
        content: String,
        existingId: String? = null,
        folderId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val metaJson = JSONObject().apply {
                put("name", fileName)
                if (existingId == null) put("parents", org.json.JSONArray().put(folderId))
            }.toString()

            val boundary  = "compass_boundary"
            val multipart = "--$boundary\r\n" +
                    "Content-Type: $MIME_JSON\r\n\r\n$metaJson\r\n" +
                    "--$boundary\r\n" +
                    "Content-Type: $MIME_JSON\r\n\r\n$content\r\n" +
                    "--$boundary--"

            val body = multipart.toRequestBody("multipart/related; boundary=$boundary".toMediaType())

            val req = if (existingId == null) {
                Request.Builder()
                    .url("$DRIVE_UPLOAD/files?uploadType=multipart&fields=id")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(body)
                    .build()
            } else {
                Request.Builder()
                    .url("$DRIVE_UPLOAD/files/$existingId?uploadType=multipart&fields=id")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .patch(body)
                    .build()
            }

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Drive error ${resp.code}: ${resp.body?.string()}"))
            }
            val fileId = JSONObject(resp.body?.string() ?: "{}").getString("id")
            Result.success(fileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── FILE DOWNLOAD ───────────────────────────────────────────

    /**
     * Download a file's content by its Drive ID.
     */
    suspend fun downloadFile(accessToken: String, fileId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$DRIVE_BASE/files/$fileId?alt=media")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Drive error ${resp.code}"))
                }
                Result.success(resp.body?.string() ?: "")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── LIST FILES ──────────────────────────────────────────────

    /**
     * List all diary JSON files in the app folder.
     * Returns list of (fileName, fileId) pairs.
     */
    suspend fun listFiles(accessToken: String, folderId: String): Result<List<Pair<String, String>>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$DRIVE_BASE/files?q='$folderId'+in+parents+and+trashed=false" +
                        "&fields=files(id,name)&orderBy=name"
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()
                val resp = client.newCall(req).execute()
                val json  = JSONObject(resp.body?.string() ?: "{}")
                val files = json.optJSONArray("files") ?: return@withContext Result.success(emptyList())
                val list  = (0 until files.length()).map {
                    val f = files.getJSONObject(it)
                    f.getString("name") to f.getString("id")
                }
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ─── DELETE FILE ─────────────────────────────────────────────

    suspend fun deleteFile(accessToken: String, fileId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url("$DRIVE_BASE/files/$fileId")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .delete()
                    .build()
                client.newCall(req).execute()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
