package com.hz_apps.autowificonnector

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hz_apps.aboutme.AboutMeActivity
import com.hz_apps.autowificonnector.database.AppDatabase
import com.hz_apps.autowificonnector.database.UserDao
import com.hz_apps.autowificonnector.database.WifiConfig
import com.hz_apps.autowificonnector.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), WifiViewAdapter.ItemListeners {
    private lateinit var bindings: ActivityMainBinding
    private lateinit var appDB: AppDatabase
    private lateinit var dao: UserDao
    private lateinit var wifiConfigData: LiveData<List<WifiConfig>>
    private val CONNECTED_WIFI_ID = "main"
    private val adapter: WifiViewAdapter by lazy {
        WifiViewAdapter(
            this@MainActivity,
            this@MainActivity
        )
    }
    private var connectJob: Job? = null

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        appDB = AppDatabase.getInstance(applicationContext)

        dao = appDB.userDao()


        bindings.recyclerView.adapter = adapter
        val itemTouchHelper = ItemTouchHelper(MyItemTouchHelper(adapter, dao))
        itemTouchHelper.attachToRecyclerView(bindings.recyclerView)


        CoroutineScope(Dispatchers.IO).launch {
            wifiConfigData = dao.getAll()

            runOnUiThread {
                wifiConfigData.observe(this@MainActivity) {

                    if (it.isEmpty()) {
                        bindings.noItemSaved.visibility = View.VISIBLE
                    } else {
                        bindings.noItemSaved.visibility = View.GONE
                    }
                    adapter.setWifiConfigList(it)

                    val connectedWifiId =
                        getSharedPreferences(CONNECTED_WIFI_ID, MODE_PRIVATE).getInt("id", -1)

                    runOnUiThread {
                        adapter.setConnectedWiFiID(connectedWifiId)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }

        bindings.floatingActionButton.setOnClickListener {
            val intent = Intent(this, AddViewActivity::class.java)
            startActivity(intent)
        }

        bindings.madeByTextView.setOnClickListener {
            try {
                val uri = Uri.parse("https://www.instagram.com/hammadbawara/")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (ignored: Exception) {
            }

        }


    }

    override fun onItemClicked(position: Int) {
        val wifiConfig = wifiConfigData.value?.get(position)
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(wifiConfig?.ssid)
        dialog.setMessage(wifiConfig?.identity)
        dialog.setPositiveButton("Connect") { dialogInterface: DialogInterface, _: Int ->
            if (connectJob != null && connectJob?.isActive == true) {
                connectJob?.cancel()
            }
            connectJob = CoroutineScope(Dispatchers.IO).launch {
                val result = connectWith(position)
                dialogInterface.dismiss()

                if (result != 0) {
                    launchConnectFailedDialog()
                }
            }
        }
        dialog.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
        }
        dialog.setNeutralButton("Delete") { _: DialogInterface, _: Int ->
            deleteItem(position)
        }
        dialog.show()
    }

    private fun launchConnectFailedDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setMessage(
            "Request Sending Failed.\n" +
                    "Solution:\n" +
                    "If you're already connected to the 'eduroam' network, go to settings and forget the 'eduroam' network. Then try again."
        )
        dialog.setTitle("Connection Request Failed")
        dialog.setPositiveButton("Open Wi-Fi Settings") { dialogInterface: DialogInterface, _: Int ->
            openWifiSettings()
            dialogInterface.dismiss()
        }
        dialog.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
        }
        dialog.show()
    }


    override fun onItemLongClick(position: Int) {
        connectWith(position)
    }

    override fun OnRightBtnClick(position: Int) {
        connectWith(position)
    }

    private fun connectWith(position: Int): Int {

        val wifiConfig: WifiConfig? = wifiConfigData.value?.get(position)
        if (wifiConfig != null) {
            var isSaved = WifiConnector.connectToEAPWifi(
                this,
                wifiConfig.ssid,
                wifiConfig.identity,
                wifiConfig.password
            )
//            if (Build.VERSION.SDK_INT < 29) {
//                isSaved =
//            }else {
//                isSaved = WifiConnector.connectToEAPWifi299(this, wifiConfig.ssid, wifiConfig.identity, wifiConfig.password)
//            }

            if (isSaved) {
                Snackbar.make(bindings.root, "Connect Request is sent", Snackbar.LENGTH_SHORT)
                    .show()
                getSharedPreferences(CONNECTED_WIFI_ID, MODE_PRIVATE).edit()
                    .putInt("id", wifiConfig.id).apply()
                runOnUiThread {
                    adapter.updateWiFiID(wifiConfig.id)
                }
                return 0
            } else {
                Snackbar.make(bindings.root, "Failed to sent request", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(bindings.root, "Something went wrong", Snackbar.LENGTH_SHORT).show()
        }
        return -1
    }

    private fun deleteItem(position: Int) {
        val wifiConfig = wifiConfigData.value?.get(position)
        CoroutineScope(Dispatchers.IO).launch {
            if (wifiConfig != null) {
                dao.deleteItem(wifiConfig.id)
            } else {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.open_Wifi) {
            openWifiSettings()
        } else if (item.itemId == R.id.about) {
            val intent = Intent(this, AboutMeActivity::class.java)
            val appName = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(
                    packageName,
                    0
                )
            )
            val version = packageManager.getPackageInfo(packageName, 0).versionName
            intent.putExtra("app_name", appName)
            intent.putExtra("version", version)
            intent.putExtra("app_icon", R.mipmap.ic_launcher)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
    }
}