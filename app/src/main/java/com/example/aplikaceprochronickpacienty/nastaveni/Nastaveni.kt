package com.example.aplikaceprochronickpacienty.nastaveni

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Ucet

class Nastaveni : AppCompatActivity() {

    // Tlačítko - zpětné vrácení
    private lateinit var nastaveni_button_zpet: ImageButton

    // Switch theme
    private lateinit var nastaveni_switch_mode: SwitchCompat

    // Tmavý motiv
    var nightMode: Boolean = false

    private lateinit var preferences: SharedPreferences

    private lateinit var editPreferences: SharedPreferences.Editor
    override fun onCreate(savedInstanceState: Bundle?) {

        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nastaveni)

        // Tlačítko Zpět
        nastaveni_button_zpet = findViewById(R.id.nastaveni_button_zpet)

        // Přesměrování uživatele na aktivitu Účet
        nastaveni_button_zpet.setOnClickListener {

            startActivity(Intent(this, Ucet::class.java))
        }

        // Dark/White mode
        nastaveni_switch_mode = findViewById(R.id.nastaveni_switch_mode)

        preferences = getSharedPreferences("MODE", Context.MODE_PRIVATE)

        nightMode = preferences.getBoolean("nightMode",false)

        if (nightMode) {

            nastaveni_switch_mode.isChecked = true
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        nastaveni_switch_mode.setOnClickListener {

            if (nightMode) {

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                editPreferences = preferences.edit()

                editPreferences.putBoolean("nightMode",false)

            } else {

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                editPreferences = preferences.edit()

                editPreferences.putBoolean("nightMode",true)
            }

            editPreferences.apply()
        }
    }
}