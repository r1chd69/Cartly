package com.rtech.cartly

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("CartlyPrefs", Context.MODE_PRIVATE)

        val btnBack = findViewById<TextView>(R.id.btnBackSettings)
        btnBack.setOnClickListener { finish() }

        val locationInput = findViewById<EditText>(R.id.locationInput)
        val savedLocation = prefs.getString("location", "Sandton, Johannesburg")
        locationInput.setText(savedLocation)

        val btnSaveLocation = findViewById<TextView>(R.id.btnSaveLocation)
        btnSaveLocation.setOnClickListener {
            val location = locationInput.text.toString()
            if (location.isNotEmpty()) {
                prefs.edit().putString("location", location).apply()
                Toast.makeText(this, "Location saved!", Toast.LENGTH_SHORT).show()
            }
        }

        val switchDarkMode = findViewById<Switch>(R.id.switchDarkMode)
        val isDark = prefs.getBoolean("darkMode", false)
        switchDarkMode.isChecked = isDark
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("darkMode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        val switchNewDeals = findViewById<Switch>(R.id.switchNewDeals)
        switchNewDeals.isChecked = prefs.getBoolean("notifyNewDeals", true)
        switchNewDeals.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifyNewDeals", isChecked).apply()
            Toast.makeText(this, if (isChecked) "New deals notifications on" else "New deals notifications off", Toast.LENGTH_SHORT).show()
        }

        val switchPriceDrops = findViewById<Switch>(R.id.switchPriceDrops)
        switchPriceDrops.isChecked = prefs.getBoolean("notifyPriceDrops", true)
        switchPriceDrops.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifyPriceDrops", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Price drop notifications on" else "Price drop notifications off", Toast.LENGTH_SHORT).show()
        }
    }
}