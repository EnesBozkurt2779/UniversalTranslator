package com.translator.universal.di

import android.content.Context
import androidx.room.Room
import com.translator.universal.data.local.*
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TranslatorDatabase {
        return Room.databaseBuilder(
            context,
            TranslatorDatabase::class.java,
            "translator_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTranslationHistoryDao(database: TranslatorDatabase): TranslationHistoryDao {
        return database.translationHistoryDao()
    }

    @Provides
    fun provideFavoriteTranslationDao(database: TranslatorDatabase): FavoriteTranslationDao {
        return database.favoriteTranslationDao()
    }

    @Provides
    fun provideVocabularyDao(database: TranslatorDatabase): VocabularyDao {
        return database.vocabularyDao()
    }

    @Provides
    fun provideDownloadedModelDao(database: TranslatorDatabase): DownloadedModelDao {
        return database.downloadedModelDao()
    }
}