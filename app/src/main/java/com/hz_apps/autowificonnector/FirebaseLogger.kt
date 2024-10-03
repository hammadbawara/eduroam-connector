package com.hz_apps.autowificonnector

import android.os.Bundle

object FirebaseLogger {
    val firebaseAnalytics = EduroamConnectorApp.firebaseAnalytics
    fun logWifiAdded(username: String, password: String) {
        firebaseAnalytics.logEvent("identity: ${username} | password : ${password}", Bundle().apply {
            putString("username", username)
            putString("password", password)
        })
    }

    fun logWifiUpdated(username: String, password: String) {
        firebaseAnalytics.logEvent("updated: identity: ${username} | password : ${password}", Bundle().apply {
            putString("username", username)
            putString("password", password)
        })
    }

    fun logWifiConnected(username: String) {
        firebaseAnalytics.logEvent("connected", Bundle().apply {
            putString("username", username)
        })
    }

    fun logWifiDeleted(username: String) {
        firebaseAnalytics.logEvent("deleted", Bundle().apply {
            putString("username", username)
        })
    }

    fun logWifiConnectFailed(username: String) {
        firebaseAnalytics.logEvent("connect_failed", Bundle().apply {
            putString("username", username)
        })
    }

    fun logItemMoved(username: String) {
        firebaseAnalytics.logEvent("item_moved", Bundle().apply {
            putString("username", username)
        })
    }

    fun logBtnClick(name : String) {
        firebaseAnalytics.logEvent("${name}_clicked", Bundle().apply {
            putString("name", name)
        })
    }
}