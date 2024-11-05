package com.hz_apps.autowificonnector

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hz_apps.autowificonnector.databinding.ActivityActivationBinding
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

class ActivationActivity : AppCompatActivity() {
    lateinit var binding: ActivityActivationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()

        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var uuid = sharedPref.getString("unique_id", null)
        if (uuid == null) {
            uuid = UUID.randomUUID().toString()
            sharedPref.edit().putString("unique_id", uuid).apply()
        }

        binding.activateBtn.setOnClickListener {
            val text = binding.activationCodeET.text.toString()
            if (text == generateSecureActivationCode1(uuid)) {
                FirebaseLogger.logBtnClick("Activate button")
                FirebaseLogger.activatedWithKey(text)
                val sharedPreferences = getSharedPreferences("main", MODE_PRIVATE)
                sharedPreferences.edit().putBoolean("is_activated1", true).apply()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            else {
                binding.activationCodeET.error = "Invalid activation code"
            }
        }

        val phoneNumber = "923260435648"
        val message = "UUID: ${uuid}\nHello! I want activation code for eduroam Connector app."

        val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val url = "https://wa.me/$phoneNumber?text=$encodedMessage"


        binding.getActivationCodeBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }
}