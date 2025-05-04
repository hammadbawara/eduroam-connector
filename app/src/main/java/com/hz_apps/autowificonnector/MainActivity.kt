package com.hz_apps.autowificonnector

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.hz_apps.aboutme.AboutMeActivity
import com.hz_apps.autowificonnector.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), WifiViewAdapter.ItemListeners , ItemTouchHelperCallback {

    private lateinit var bindings: ActivityMainBinding
    private val adapter: WifiViewAdapter by lazy {
        WifiViewAdapter(this@MainActivity, this@MainActivity)
    }
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(bindings.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bindings = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        if (!viewModel.isActivated) {
            val intent = Intent(this, ActivationActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Setup RecyclerView
        bindings.recyclerView.adapter = adapter
        val itemTouchHelper = ItemTouchHelper(MyItemTouchHelper { fromPosition, toPosition ->
            viewModel.onItemMove(fromPosition, toPosition)
            viewModel.updateWiFiConfigOrder()
            adapter.onItemMove(fromPosition, toPosition)
            FirebaseLogger.logItemMoved("item_moved")
        })
        itemTouchHelper.attachToRecyclerView(bindings.recyclerView)

        viewModel.wifiConfigData.observe(this) { configList ->

            if (viewModel.shouldUpload) {
                viewModel.uploadConfig(configList)
                viewModel.setShouldUploadFalse()
            }
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

        viewModel.connectTimes.observe(this) {
            bindings.connectTimes.text = "Connected: $it"
        }

        viewModel.connectedWifiId.observe(this) {
            adapter.updateWiFiID(it)
        }

        viewModel.showProgressBar.observe(this){ show->
            val visibility = if (show) View.VISIBLE else View.GONE
            bindings.progressBar.visibility = visibility
        }
    }

    private fun connectAndShowSnackBar(position: Int) {
        viewModel.connectWith(position,
            onSuccess = { id->
                Snackbar.make(bindings.root, "Connect Request is sent", Snackbar.LENGTH_SHORT).show()
            },
            onError = {
                Snackbar.make(bindings.root, it, Snackbar.LENGTH_SHORT).show()
            }
        )
    }

    private fun deleteItem(position: Int) {
        val wifiConfig = viewModel.wifiConfigData.value?.get(position)
        viewModel.deleteItem(wifiConfig) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            FirebaseLogger.logWifiDeleted(wifiConfig?.identity ?: "")
        }
    }

    override fun onItemClicked(position: Int) {
//        val wifiConfig = viewModel.wifiConfigData.value?.get(position)
//        val dialog = MaterialAlertDialogBuilder(this)
//        dialog.setTitle(wifiConfig?.ssid)
//        dialog.setMessage(wifiConfig?.identity)
//        dialog.setPositiveButton("Connect") { dialogInterface: DialogInterface, _: Int ->
//            connectAndShowSnackBar(position)
//            dialogInterface.dismiss()
//        }
//        dialog.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
//            dialogInterface.dismiss()
//            FirebaseLogger.logBtnClick("Click Dialog Btn")
//        }
//        dialog.show()
        connectAndShowSnackBar(position)
    }

    override fun onItemLongClick(position: Int) {
        onItemClicked(position)
    }

    override fun onMoreOptionClicked(position: Int, view: View) {
        val popMenuStyle = TypedValue()
        this.theme.resolveAttribute(com.google.android.material.R.attr.popupMenuStyle, popMenuStyle, true)

        val popupMenu = PopupMenu(this, view, 0, 0, popMenuStyle.resourceId)
        popupMenu.menu.add("Connect")
        if (Build.VERSION.SDK_INT >= 29 && viewModel.isConnected(position)) popupMenu.menu.add("Disconnect")
        popupMenu.menu.add("Edit")
        popupMenu.menu.add("Delete")
        popupMenu.setOnMenuItemClickListener {
            when (it.title) {
                "Connect" -> {
                    connectAndShowSnackBar(position)
                }
                "Disconnect" -> {
                    if (Build.VERSION.SDK_INT >= 29) {
                        viewModel.disconnectWifi(position)
                    }
                }
                "Edit" -> {
                    launchEditWifiConfigActivity(position)
                }
                "Delete" -> {
                    deleteItem(position)
                }
            }
            true
        }
        popupMenu.show()
    }

    private fun launchEditWifiConfigActivity(position: Int) {
        val wifiConfig = viewModel.wifiConfigData.value?.get(position)!!
        if (wifiConfig.isEditAllowed) {
            val intent = Intent(this, AddViewActivity::class.java)
            intent.putExtra("id", wifiConfig.id)
            intent.putExtra("ssid", wifiConfig.ssid)
            intent.putExtra("identity", wifiConfig.identity)
            intent.putExtra("password", wifiConfig.password)
            intent.putExtra("domain", wifiConfig.domain)
            startActivity(intent)
        } else {
            Toast.makeText(this, "You can't edit this", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.open_Wifi -> openWifiSettings()
            R.id.about -> openAboutActivity()
            R.id.import_from_csv -> importFromCSV()
            R.id.export_to_csv -> exportToCSV()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun importFromCSV() {
        Toast.makeText(this, "Wait for the next update", Toast.LENGTH_SHORT).show()
    }

    private fun exportToCSV() {
        Toast.makeText(this, "Wait for the next update", Toast.LENGTH_SHORT).show()
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
