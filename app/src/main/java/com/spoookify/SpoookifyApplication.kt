package com.spoookify

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe

@HiltAndroidApp
class SpoookifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            NewPipe.init(YoutubeExtractorHelper.getInstance())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
