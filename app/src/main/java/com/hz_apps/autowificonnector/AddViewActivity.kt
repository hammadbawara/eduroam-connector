package com.hz_apps.autowificonnector

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hz_apps.autowificonnector.database.AppDatabase
import com.hz_apps.autowificonnector.database.WifiConfig
import com.hz_apps.autowificonnector.databinding.ActivityAddViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddViewActivity : AppCompatActivity() {
    private lateinit var bindings : ActivityAddViewBinding
    private var id = 0
    private var ssid = ""
    private var identity = ""
    private var password = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityAddViewBinding.inflate(layoutInflater)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(bindings.root)

        id = intent.getIntExtra("id", 0)
        if (id != -0) {
            ssid = intent.getStringExtra("ssid")!!
            identity = intent.getStringExtra("identity")!!
            password = intent.getStringExtra("password")!!

            bindings.ssidTv.setText(ssid)
            bindings.identityTv.setText(identity)
            bindings.passwordTv.setText(password)

            bindings.saveBtn.text = "Update"
            supportActionBar?.title = "Update Item"
        }else {
            supportActionBar?.title = "Add Item"
        }

        bindings.saveBtn.setOnClickListener {
            saveIntoDatabase()
        }

    }

    private fun saveIntoDatabase() {
        val ssid = "eduroam"
        val identity = bindings.identityTv.text.toString()
        val password = bindings.passwordTv.text.toString()

        bindings.identityTv.error = null
        bindings.ssidTv.error = null
        bindings.passwordTv.error = null
        if (ssid.isBlank()) {
            bindings.identityTv.error = "SSID should not be blank"
            return
        }
        if (identity.isBlank()) {
            bindings.identityTv.error = "Identity should not be blank"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(identity).matches()) {
            bindings.identityTv.error = "Enter correct email"
            return
        }
        if (password.isBlank()) {
            bindings.passwordTv.error = "Password should not be blank"
            return
        }
        if(password.length < 8) {
            bindings.passwordTv.error = "Password length must be 8 letters"
            return
        }

        val db = AppDatabase.getInstance(applicationContext)
        val dbDao = db.userDao()
        val wifiConfig : WifiConfig?
        if (id == 0) {
            wifiConfig = WifiConfig(ssid = ssid, identity = identity, password = password, isEditAllowed = true)
        }else {
            wifiConfig = WifiConfig(id, ssid, identity, password, true)
        }
        CoroutineScope(Dispatchers.IO).launch {
            dbDao.insert(wifiConfig)
            runOnUiThread {
                if (id == 0) {
                    Toast.makeText(this@AddViewActivity, "Added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AddViewActivity, "Updated", Toast.LENGTH_SHORT).show()
                }
            }
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val identity = bindings.identityTv.text.toString()
        val password = bindings.passwordTv.text.toString()
        if (ssid == this.ssid && identity == this.identity && password == this.password){
            onBackPressed()
        }else {
            askBeforeExiting()
        }

        return super.onSupportNavigateUp()
    }

    private fun askBeforeExiting() {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setMessage("Do you want to discard changes?")
        dialog.setPositiveButton("Discard") { dialogInterface: DialogInterface, i: Int ->
            onBackPressed()
        }
        dialog.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }
        dialog.show()
    }
}