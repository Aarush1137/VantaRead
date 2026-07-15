package com.example.vantaread

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.vantaread.worker.DownloadNotifications
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VantaReadApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var preferencesManager: com.example.vantaread.data.prefs.ReaderPreferencesManager
    @Inject lateinit var novelRepository: com.example.vantaread.data.repository.NovelRepository

    override fun onCreate() {
        super.onCreate()
        
        // Initialize storage location
        val storageUri = preferencesManager.storageUri.value
        novelRepository.setStorageUri(storageUri)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DownloadNotifications.CHANNEL_ID,
                DownloadNotifications.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
