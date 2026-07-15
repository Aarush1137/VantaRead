package com.example.vantaread.di

import android.content.Context
import androidx.room.Room
import com.example.vantaread.data.db.AppDatabase
import com.example.vantaread.data.db.NovelDao
import com.example.vantaread.data.db.ReadingHistoryDao
import com.example.vantaread.data.repository.NovelRepository
import com.example.vantaread.data.source.NovelSource
import com.example.vantaread.data.source.lightnovelpub.LightNovelPubSource
import com.example.vantaread.data.source.novelfull.NovelFullSource
import com.example.vantaread.data.source.royalroad.RoyalRoadSource
import com.example.vantaread.data.source.freewebnovel.FreeWebNovelSource
import com.example.vantaread.data.source.scribblehub.ScribbleHubSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vantaread_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideNovelDao(appDatabase: AppDatabase): NovelDao {
        return appDatabase.novelDao()
    }

    @Provides
    fun provideReadingHistoryDao(appDatabase: AppDatabase): ReadingHistoryDao {
        return appDatabase.readingHistoryDao()
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): androidx.work.WorkManager {
        return androidx.work.WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideNovelSources(@ApplicationContext context: Context): Map<String, @JvmSuppressWildcards NovelSource> {
        val novelFullSource = NovelFullSource(context)
        val royalRoadSource = RoyalRoadSource(context)
        val lightNovelPubSource = LightNovelPubSource(context)
        val freeWebNovelSource = FreeWebNovelSource(context)
        val scribbleHubSource = ScribbleHubSource(context)
        
        return mapOf(
            novelFullSource.sourceId to novelFullSource,
            royalRoadSource.sourceId to royalRoadSource,
            lightNovelPubSource.sourceId to lightNovelPubSource,
            freeWebNovelSource.sourceId to freeWebNovelSource,
            scribbleHubSource.sourceId to scribbleHubSource
        )
    }

    @Provides
    @Singleton
    fun provideNovelRepository(sources: Map<String, @JvmSuppressWildcards com.example.vantaread.data.source.NovelSource>, novelDao: NovelDao, readingHistoryDao: ReadingHistoryDao): NovelRepository {
        return NovelRepository(sources, novelDao, readingHistoryDao)
    }
}
