package com.example.vantaread.di

import android.content.Context
import androidx.room.Room
import com.example.vantaread.data.db.AppDatabase
import com.example.vantaread.data.db.NovelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

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
    fun provideNovelDao(database: AppDatabase): NovelDao {
        return database.novelDao()
    }
}
