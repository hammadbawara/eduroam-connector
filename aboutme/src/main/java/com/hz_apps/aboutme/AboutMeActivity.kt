package com.hz_apps.aboutme

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView

class AboutMeActivity : AppCompatActivity() {
    private var action = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_me)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About"

        val appName = intent.getStringExtra("app_name")
        val version = intent.getStringExtra("version")
        val appIcon = intent.getIntExtra("app_icon", 0)

        val github = findViewById<ImageButton>(R.id.github_iv)
        val facebook = findViewById<ImageButton>(R.id.facebook_iv)
        val linkedin = findViewById<ImageButton>(R.id.linkedin_iv)
        val instagram = findViewById<ImageButton>(R.id.instagram_iv)

        val appNameTV = findViewById<TextView>(R.id.textView5)
        val versionTV = findViewById<TextView>(R.id.textView6)
        val appIconIV = findViewById<ShapeableImageView>(R.id.icon_of_app)

        appNameTV.text = appName
        versionTV.text = version
        appIconIV.setImageResource(appIcon)


        github.setOnClickListener {
            action = "https://github.com/hammadbawara"
            openUrl()
        }

        facebook.setOnClickListener {
            action = "https://www.facebook.com/hammadbawra"
            openUrl()
        }

        linkedin.setOnClickListener {
            action ="https://www.linkedin.com/in/hammadbawara/"
            openUrl()
        }

        instagram.setOnClickListener {
            action = "https://www.instagram.com/hammadbawara/"
            openUrl()
        }
    }

    private fun openUrl() {
        try{
            if (action != "") {
                val uri = Uri.parse(action)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }catch (ignored : Exception) {
            Toast.makeText(this, "No app found for this action", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}