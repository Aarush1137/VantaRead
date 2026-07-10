package com.example.vantaread.worker

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.vantaread.data.db.NovelDao
import com.example.vantaread.data.repository.NovelRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class ChapterDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val novelRepository: NovelRepository,
    private val novelDao: NovelDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL) ?: return@withContext Result.failure()
        val sourceId = inputData.getString(KEY_SOURCE_ID) ?: return@withContext Result.failure()
        val inputNovelUrl = inputData.getString(KEY_NOVEL_URL)
        val inputNovelTitle = inputData.getString(KEY_NOVEL_TITLE) ?: "Novel"
        val inputChapterTitle = inputData.getString(KEY_CHAPTER_TITLE) ?: "Chapter"
        publishWorkProgress(inputNovelTitle, inputChapterTitle, "Queued")

        try {
            Log.d("DownloadWorker", "Starting download for $chapterUrl")
            val chapterEntity = novelDao.getChapter(chapterUrl) ?: return@withContext Result.failure()
            val novelUrl = inputNovelUrl ?: chapterEntity.novelUrl
            val novelTitle = novelDao.getNovel(novelUrl)?.title ?: inputNovelTitle
            publishWorkProgress(novelTitle, inputChapterTitle, "Downloading")
            notifyProgress(
                novelUrl = novelUrl,
                novelTitle = novelTitle,
                chapterTitle = inputChapterTitle,
                status = "Downloading"
            )
            
            if (chapterEntity.isDownloaded && !chapterEntity.content.isNullOrBlank()) {
                Log.d("DownloadWorker", "Chapter already downloaded")
                notifyProgress(
                    novelUrl = novelUrl,
                    novelTitle = novelTitle,
                    chapterTitle = inputChapterTitle,
                    status = "Already downloaded"
                )
                return@withContext Result.success()
            }

            // Fetch from network
            val content = novelRepository.getChapterContent(chapterUrl, sourceId)
            if (
                content.isBlank() ||
                content.startsWith("Error loading chapter content") ||
                content.startsWith("Failed to")
            ) {
                Log.e("DownloadWorker", "Downloaded content was empty or failed for $chapterUrl")
                notifyProgress(
                    novelUrl = novelUrl,
                    novelTitle = novelTitle,
                    chapterTitle = inputChapterTitle,
                    status = "Retrying"
                )
                return@withContext Result.retry()
            }
            
            // Update database
            val updatedChapter = chapterEntity.copy(
                content = content,
                isDownloaded = true
            )
            novelDao.insertChapter(updatedChapter)
            notifyProgress(
                novelUrl = novelUrl,
                novelTitle = novelTitle,
                chapterTitle = inputChapterTitle,
                status = "Downloaded"
            )
            
            Log.d("DownloadWorker", "Successfully downloaded $chapterUrl")
            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Failed to download $chapterUrl", e)
            publishWorkProgress(inputNovelTitle, inputChapterTitle, "Retrying")
            notifyFailure(inputNovelUrl ?: chapterUrl, inputNovelTitle, inputChapterTitle)
            Result.retry()
        }
    }

    private suspend fun publishWorkProgress(
        novelTitle: String,
        chapterTitle: String,
        state: String,
        progressText: String = ""
    ) {
        val progressData = Data.Builder()
            .putString(KEY_NOVEL_TITLE, novelTitle)
            .putString(KEY_CHAPTER_TITLE, chapterTitle)
            .putString(KEY_DOWNLOAD_STATE, state)
            .putString(KEY_DOWNLOAD_PROGRESS_TEXT, progressText)
            .build()
        setProgress(progressData)
    }

    private suspend fun notifyProgress(
        novelUrl: String,
        novelTitle: String,
        chapterTitle: String,
        status: String
    ) {
        val total = novelDao.getChapterCountForNovel(novelUrl).coerceAtLeast(1)
        val downloaded = novelDao.getDownloadedChapterCountForNovel(novelUrl)
        val progressText = "$downloaded/$total chapters downloaded"
        publishWorkProgress(novelTitle, chapterTitle, status, progressText)

        if (!canNotify()) return

        val title = if (downloaded >= total) "Downloads complete" else "$status: $novelTitle"

        val notification = NotificationCompat.Builder(context, DownloadNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText("$chapterTitle - $progressText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$chapterTitle\n$progressText"))
            .setOnlyAlertOnce(true)
            .setOngoing(downloaded < total)
            .setProgress(total, downloaded.coerceAtMost(total), false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(notificationIdFor(novelUrl), notification)
    }

    private fun notifyFailure(novelUrl: String, novelTitle: String, chapterTitle: String) {
        if (!canNotify()) return

        val notification = NotificationCompat.Builder(context, DownloadNotifications.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download retrying: $novelTitle")
            .setContentText(chapterTitle)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(notificationIdFor(novelUrl), notification)
    }

    private fun canNotify(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun notificationIdFor(novelUrl: String): Int {
        return novelUrl.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
    }

    companion object {
        const val TAG_DOWNLOAD = "chapter_download"
        const val KEY_CHAPTER_URL = "chapter_url"
        const val KEY_SOURCE_ID = "source_id"
        const val KEY_NOVEL_URL = "novel_url"
        const val KEY_NOVEL_TITLE = "novel_title"
        const val KEY_CHAPTER_TITLE = "chapter_title"
        const val KEY_DOWNLOAD_STATE = "download_state"
        const val KEY_DOWNLOAD_PROGRESS_TEXT = "download_progress_text"

        fun tagForNovel(novelUrl: String): String = "chapter_download_novel_$novelUrl"
    }
}
