package com.hz_apps.autowificonnector

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hz_apps.autowificonnector.database.AppDatabase
import com.hz_apps.autowificonnector.database.UserDao
import com.hz_apps.autowificonnector.database.WifiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appDB: AppDatabase = AppDatabase.getInstance(application)
    private val dao: UserDao = appDB.userDao()
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("main", Application.MODE_PRIVATE)
    var shouldUpload = false
    private set

    private val _showProgressBar = MutableLiveData(false)
    val showProgressBar: LiveData<Boolean>
        get() = _showProgressBar

    private val _connectTimes = MutableLiveData(0)
    val connectTimes: LiveData<Int>
        get() = _connectTimes

    private val _connectedWifiId = MutableLiveData(-1)
    val connectedWifiId: LiveData<Int>
        get() = _connectedWifiId

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

        _connectTimes.value = getConnectedTimes()

        _connectedWifiId.value = getConnectedWifiId()
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

    private fun connectWith(wifiConfig: WifiConfig) : Boolean{
        val isConnected = WifiConnector.connectToEAPWifi(
            getApplication(),
            wifiConfig.ssid,
            wifiConfig.identity,
            wifiConfig.password
        )
        return isConnected
    }

    @RequiresApi(29)
    private fun connectWith29(wifiConfig: WifiConfig) : Boolean {
        val wifiConnector = WifiConnectorN()
        disconnectWifi(getConnectedWifi())
        val isConnected = wifiConnector
            .connectToWifi(
                getApplication(),
                wifiConfig.identity,
                wifiConfig.password,
                wifiConfig.domain
            )
        return isConnected
    }

    private fun showProgressBar() {
        viewModelScope.launch(Dispatchers.Main){
            if (_showProgressBar.value == false)
                _showProgressBar.value = true
        }

    }

    private fun hideProgressBar() {
        viewModelScope.launch(Dispatchers.Main) {
            if (_showProgressBar.value == true)
                _showProgressBar.value = false
        }
    }

    fun connectWith(position: Int, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            showProgressBar()
            val wifiConfig = wifiConfigList.getOrNull(position)
            if (wifiConfig != null) {
                val isConnected = if (Build.VERSION.SDK_INT < 29)
                    connectWith(wifiConfig)
                else
                    connectWith29(wifiConfig)

                withContext(Dispatchers.Main) {
                    if (isConnected) {
                        onSuccess(wifiConfig.id)
                        _connectTimes.value = _connectTimes.value?.plus(1)
                        _connectedWifiId.value = wifiConfig.id
                        updateConnectTimesIDInSP()
                        FirebaseLogger.logWifiConnected(wifiConfig.identity)
                    } else {
                        onError("Failed to send connect request")
                        FirebaseLogger.logWifiConnectFailed(wifiConfig.identity)
                    }
                }

            } else {
                withContext(Dispatchers.Main) {
                    onError("Invalid Wi-Fi configuration")
                }
            }
            hideProgressBar()
        }
    }

    private fun getConnectedWifi() : WifiConfig? {
        val connectedWifiID = getConnectedWifiId()
        if (connectedWifiID != -1) {
            return wifiConfigList[getConnectedWifiId()]
        }
        return null
    }
    // Update connect ID and connect times in shared preference
    private fun updateConnectTimesIDInSP() {
        with(sharedPreferences.edit()) {
            connectedWifiId.value?.let { putInt("id", it) }
            _connectTimes.value?.let { putInt("connect_times", it) }
            apply()
        }
    }

    private fun getConnectedWifiId() : Int {
        val id = sharedPreferences.getInt("id", -1)
        return id
    }

    private fun getConnectedTimes(): Int {
        return sharedPreferences.getInt("connect_times", 0)
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

    @RequiresApi(29)
    fun disconnectWifi(wifiConfig: WifiConfig?) {
        if (wifiConfig != null) {
            val wifiConnector = WifiConnectorN()
            wifiConnector.removeNetwork(
                getApplication(),
                wifiConfig.identity,
                wifiConfig.password,
                wifiConfig.domain
            )
        }

    }

    @RequiresApi(29)
    fun disconnectWifi(position: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            showProgressBar()
            val wifiConfig = wifiConfigData.value!![position]
            disconnectWifi(wifiConfig)
            hideProgressBar()
        }
        _connectedWifiId.value = -1
        updateConnectTimesIDInSP()
    }

    fun isConnected(position: Int): Boolean {
        val wifiConfig = wifiConfigData.value!![position]
        return wifiConfig.id == connectedWifiId.value
    }
}
