package com.translator.universal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UniversalTranslatorApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}