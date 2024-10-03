package com.hz_apps.autowificonnector

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.hz_apps.aboutme.AboutMeActivity
import com.hz_apps.autowificonnector.database.WifiConfig
import com.hz_apps.autowificonnector.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), WifiViewAdapter.ItemListeners , ItemTouchHelperCallback {

    private lateinit var bindings: ActivityMainBinding
    private val adapter: WifiViewAdapter by lazy {
        WifiViewAdapter(this@MainActivity, this@MainActivity)
    }
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Setup RecyclerView
        bindings.recyclerView.adapter = adapter
        val itemTouchHelper = ItemTouchHelper(MyItemTouchHelper { fromPosition, toPosition ->
            viewModel.onItemMove(fromPosition, toPosition) // Notify ViewModel of the move
            viewModel.updateWiFiConfigOrder() // Update the database
            adapter.onItemMove(fromPosition, toPosition) // Notify the adapter of the move
            FirebaseLogger.logItemMoved("item_moved")
        })
        itemTouchHelper.attachToRecyclerView(bindings.recyclerView)

        viewModel.wifiConfigData.observe(this) { configList ->
            if (configList.isEmpty()) {
                bindings.noItemSaved.visibility = View.VISIBLE
            } else {
                bindings.noItemSaved.visibility = View.GONE
            }
            adapter.setWifiConfigList(configList)

        }

        bindings.floatingActionButton.setOnClickListener {
            val intent = Intent(this, AddViewActivity::class.java)
            startActivity(intent)
        }

        bindings.madeByTextView.setOnClickListener {
            try {
                FirebaseLogger.logBtnClick("Instagram ID clicked")
                val uri = Uri.parse("https://www.instagram.com/hammadbawara/")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (ignored: Exception) {
            }
        }

        // Set connected times on screen
        viewModel.setConnectedTimes {
            bindings.connectTimes.text = "Connected: $it"
        }
    }

    override fun onItemClicked(position: Int) {
        val wifiConfig = viewModel.wifiConfigData.value?.get(position)
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(wifiConfig?.ssid)
        dialog.setMessage(wifiConfig?.identity)
        dialog.setPositiveButton("Connect") { dialogInterface: DialogInterface, _: Int ->
            viewModel.connectWith(position, viewModel.wifiConfigData.value ?: emptyList(),
                onSuccess = {
                    adapter.updateWiFiID(it)
                    Snackbar.make(bindings.root, "Connect Request is sent", Snackbar.LENGTH_SHORT).show()
                    FirebaseLogger.logWifiConnected(wifiConfig?.identity ?: "")
                },
                onError = {
                    Snackbar.make(bindings.root, it, Snackbar.LENGTH_SHORT).show()
                    FirebaseLogger.logWifiConnectFailed(wifiConfig?.identity ?: "")
                }
            )
            dialogInterface.dismiss()
        }
        dialog.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
            FirebaseLogger.logBtnClick("Click Dialog Btn")
        }
        dialog.setNeutralButton("Delete") { _: DialogInterface, _: Int ->
            viewModel.deleteItem(wifiConfig) {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                FirebaseLogger.logWifiDeleted(wifiConfig?.identity ?: "")
            }
        }
        dialog.show()
    }

    override fun onItemLongClick(position: Int) {
        onItemClicked(position)
    }

    override fun OnRightBtnClick(position: Int) {
        onItemClicked(position)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.open_Wifi -> openWifiSettings()
            R.id.about -> openAboutActivity()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openWifiSettings() {
        FirebaseLogger.logBtnClick("Open Wifi Settings")
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
    }

    private fun openAboutActivity() {
        FirebaseLogger.logBtnClick("About Me")
        val intent = Intent(this, AboutMeActivity::class.java)
        startActivity(intent)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {

    }

    override fun onItemMoved() {

    }
}
