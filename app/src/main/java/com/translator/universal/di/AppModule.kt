package com.translator.universal.di

import android.content.Context
import com.translator.universal.data.service.ImageTextRecognitionService
import com.translator.universal.data.service.NetworkService
import com.translator.universal.data.service.SpeechRecognitionService
import com.translator.universal.data.service.TextToSpeechService
import com.translator.universal.data.service.TranslationService
import com.translator.universal.utils.PreferencesManager
import com.translator.universal.utils.ThemeManager
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
    fun provideTranslationService(
        @ApplicationContext context: Context
    ): TranslationService = TranslationService(context)

    @Provides
    @Singleton
    fun provideNetworkService(
        @ApplicationContext context: Context
    ): NetworkService = NetworkService(context)

    @Provides
    @Singleton
    fun provideImageTextRecognitionService(
        @ApplicationContext context: Context
    ): ImageTextRecognitionService = ImageTextRecognitionService(context)

    @Provides
    @Singleton
    fun provideSpeechRecognitionService(
        @ApplicationContext context: Context
    ): SpeechRecognitionService = SpeechRecognitionService(context)

    @Provides
    @Singleton
    fun provideTextToSpeechService(
        @ApplicationContext context: Context
    ): TextToSpeechService = TextToSpeechService(context)

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager = PreferencesManager(context)

    @Provides
    @Singleton
    fun provideThemeManager(
        @ApplicationContext context: Context
    ): ThemeManager = ThemeManager(context)
}