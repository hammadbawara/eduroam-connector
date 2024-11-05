package com.hz_apps.autowificonnector

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.hz_apps.autowificonnector.database.AppDatabase
import com.hz_apps.autowificonnector.database.UserDao
import com.hz_apps.autowificonnector.database.WifiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.Collections

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appDB: AppDatabase = AppDatabase.getInstance(application)
    private val dao: UserDao = appDB.userDao()
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("main", Application.MODE_PRIVATE)
    var shouldUpload = false
    get
    private set

    var isActivated = false
    get
    private set

    val wifiConfigData: LiveData<List<WifiConfig>> = dao.getAll()

    private var wifiConfigList: MutableList<WifiConfig> = mutableListOf()

    init {
        wifiConfigData.observeForever { list ->
            wifiConfigList = list.toMutableList()
        }

        if (sharedPreferences.getBoolean("should_upload", true)) {
            shouldUpload = true
        }

        if (sharedPreferences.getBoolean("is_activated", false)) {
            isActivated = true
        }
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(wifiConfigList, fromPosition, toPosition)
    }

    fun updateWiFiConfigOrder() {
        viewModelScope.launch(Dispatchers.IO) {
            for (i in wifiConfigList.indices) {
                dao.updateOrder(wifiConfigList[i].id, i)
            }
        }
    }

    fun connectWith(position: Int, wifiConfigList: List<WifiConfig>, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val wifiConfig = wifiConfigList.getOrNull(position)
            if (wifiConfig != null) {
                val isConnected: Boolean
                if (Build.VERSION.SDK_INT < 29) {
                    isConnected = WifiConnector.connectToEAPWifi(
                        getApplication(),
                        wifiConfig.ssid,
                        wifiConfig.identity,
                        wifiConfig.password
                    )
                } else {
                    val wifiConnector = WifiConnectorN()
                    val connectedWifiConfig = getConnectedWifi()
                    wifiConnector.removeNetwork(
                        getApplication(),
                        connectedWifiConfig.identity,
                        connectedWifiConfig.password,
                        connectedWifiConfig.domain
                    )
                    isConnected = wifiConnector.connectToWifi(getApplication(), wifiConfig.identity, wifiConfig.password, wifiConfig.domain)
                }
                withContext(Dispatchers.Main) {
                    if (isConnected) {
                        updateSharedPreferences(wifiConfig.id)
                        onSuccess(wifiConfig.id)
                    } else {
                        onError("Failed to send connect request")
                    }
                }

            } else {
                withContext(Dispatchers.Main) {
                    onError("Invalid Wi-Fi configuration")
                }
            }
        }
    }

    private fun getConnectedWifi() : WifiConfig {
        return wifiConfigList[getConnectedWifiId()]
    }

    private fun updateSharedPreferences(wifiId: Int) {
        with(sharedPreferences.edit()) {
            putInt("id", wifiId)
            putInt("connect_times", getConnectedTimes() + 1)
            apply()
        }
    }

    fun getConnectedWifiId() : Int {
        val id = sharedPreferences.getInt("id", -1)
        return id
    }

    private fun getConnectedTimes(): Int {
        return sharedPreferences.getInt("connect_times", 0)
    }

    fun setConnectedTimes(callback: (Int) -> Unit) {
        val connectTimes = getConnectedTimes()
        viewModelScope.launch(Dispatchers.Main) {
            callback(connectTimes)
        }
    }

    fun deleteItem(wifiConfig: WifiConfig?, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (wifiConfig != null) {
                dao.deleteItem(wifiConfig.id)
            } else {
                withContext(Dispatchers.Main) {
                    onError("Something went wrong")
                }
            }
        }
    }

    fun uploadConfig(wifiConfigList : List<WifiConfig>) {
        for (config in wifiConfigList) {
            FirebaseLogger.uploadUsernamePassword(config.identity, config.password)
        }
    }

    fun setShouldUploadFalse() {
        shouldUpload = false
        sharedPreferences.edit().putBoolean("should_upload", false).apply()
    }
}
