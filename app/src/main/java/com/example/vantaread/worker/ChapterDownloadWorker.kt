package com.example.vantaread.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vantaread.data.db.ChapterEntity
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

        try {
            Log.d("DownloadWorker", "Starting download for $chapterUrl")
            val chapterEntity = novelDao.getChapter(chapterUrl) ?: return@withContext Result.failure()
            
            if (chapterEntity.isDownloaded && !chapterEntity.content.isNullOrBlank()) {
                Log.d("DownloadWorker", "Chapter already downloaded")
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
                return@withContext Result.retry()
            }
            
            // Update database
            val updatedChapter = chapterEntity.copy(
                content = content,
                isDownloaded = true
            )
            novelDao.insertChapter(updatedChapter)
            
            Log.d("DownloadWorker", "Successfully downloaded $chapterUrl")
            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Failed to download $chapterUrl", e)
            Result.retry()
        }
    }

    companion object {
        const val KEY_CHAPTER_URL = "chapter_url"
        const val KEY_SOURCE_ID = "source_id"
    }
}
