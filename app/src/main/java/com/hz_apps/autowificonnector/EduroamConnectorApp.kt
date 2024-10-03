package com.hz_apps.autowificonnector
import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics

class EduroamConnectorApp : Application() {
    companion object {
        lateinit var firebaseAnalytics: FirebaseAnalytics
    }
    override fun onCreate() {
        super.onCreate()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

    }
}