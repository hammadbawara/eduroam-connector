package com.hz_apps.autowificonnector

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
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

    val wifiConfigData: LiveData<List<WifiConfig>> = dao.getAll()

    // Mutable list to hold the current list of WiFi configs (updateable within ViewModel)
    private var wifiConfigList: MutableList<WifiConfig> = mutableListOf()

    init {
        // Observe the LiveData and update the internal list whenever the data changes
        wifiConfigData.observeForever { list ->
            wifiConfigList = list.toMutableList()
        }
    }

    // Function to handle item move
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        // Swap items in the list
        Collections.swap(wifiConfigList, fromPosition, toPosition)
    }

    // Function to update the WiFi config order in the database
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
                val isConnected = WifiConnector.connectToEAPWifi(
                    getApplication(),
                    wifiConfig.ssid,
                    wifiConfig.identity,
                    wifiConfig.password
                )
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

    private fun updateSharedPreferences(wifiId: Int) {
        with(sharedPreferences.edit()) {
            putInt("id", wifiId)
            putInt("connect_times", getConnectedTimes() + 1)
            apply()
        }
    }

    fun getConnectedTimes(): Int {
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
}
