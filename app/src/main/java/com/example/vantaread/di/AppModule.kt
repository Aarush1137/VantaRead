package com.example.vantaread.di

import android.content.Context
import androidx.room.Room
import com.example.vantaread.data.db.AppDatabase
import com.example.vantaread.data.db.NovelDao
import com.example.vantaread.data.repository.NovelRepository
import com.example.vantaread.data.source.wtrlab.WtrLabSource
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
        ).build()
    }

    @Provides
    fun provideNovelDao(appDatabase: AppDatabase): NovelDao {
        return appDatabase.novelDao()
    }

    @Provides
    @Singleton
    fun provideNovelSources(@ApplicationContext context: Context): Map<String, @JvmSuppressWildcards com.example.vantaread.data.source.NovelSource> {
        val wtrLab = WtrLabSource()
        val novelFull = com.example.vantaread.data.source.novelfull.NovelFullSource(context)
        return mapOf(
            wtrLab.sourceId to wtrLab,
            novelFull.sourceId to novelFull
        )
    }

    @Provides
    @Singleton
    fun provideNovelRepository(sources: Map<String, @JvmSuppressWildcards com.example.vantaread.data.source.NovelSource>, novelDao: NovelDao): NovelRepository {
        return NovelRepository(sources, novelDao)
    }
}
