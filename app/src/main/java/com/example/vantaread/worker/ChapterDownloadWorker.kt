package com.example.vantaread.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vantaread.data.db.NovelDao
import com.example.vantaread.data.db.ChapterEntity
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
            // Fetch content
            val content = novelRepository.getChapterContent(chapterUrl, sourceId)
            
            // Save to DB
            val chapter = novelDao.getChapter(chapterUrl)
            if (chapter != null) {
                novelDao.insertChapter(chapter.copy(content = content, isDownloaded = true))
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val KEY_CHAPTER_URL = "chapter_url"
        const val KEY_SOURCE_ID = "source_id"
    }
}
