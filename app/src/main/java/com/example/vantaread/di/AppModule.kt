package com.example.vantaread.di

import android.content.Context
import androidx.room.Room
import com.example.vantaread.data.db.AppDatabase
import com.example.vantaread.data.db.NovelDao
import com.example.vantaread.data.repository.NovelRepository
import com.example.vantaread.data.source.NovelSource
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
    fun provideNovelSource(): NovelSource {
        return WtrLabSource()
    }

    @Provides
    @Singleton
    fun provideNovelRepository(novelSource: NovelSource, novelDao: NovelDao): NovelRepository {
        return NovelRepository(novelSource, novelDao)
    }
}
