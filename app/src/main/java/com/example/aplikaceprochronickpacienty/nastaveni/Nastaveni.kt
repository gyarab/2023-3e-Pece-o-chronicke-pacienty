package com.example.aplikaceprochronickpacienty.nastaveni

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Ucet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Nastaveni : AppCompatActivity() {

    // Tlačítko - zpětné vrácení
    private lateinit var nastaveni_button_zpet: ImageButton

    // Switch theme
    private lateinit var nastaveni_switch_mode: SwitchCompat
    var darkModeZapnut: Boolean = true

    // Switch oznámení
    private lateinit var nastaveni_switch_oznameni: SwitchCompat
    var oznameniZapnuta: Boolean = true

    // Switch oznámení
    private lateinit var nastaveni_kroky_editext: EditText
    private lateinit var nastaveni_vaha_editext: EditText
    private lateinit var nastaveni_datum_narozeni_editext: EditText
    private lateinit var nastaveni_vyska_editext: EditText
    private lateinit var nastaveni_vaha_udaje_editext: EditText

    // Firebase Realtime database
    private lateinit var databazeFirebase: FirebaseDatabase
    private lateinit var referenceFirebaseUzivatel: DatabaseReference

    // Uživatel Firebase Auth
    private lateinit var uzivatel: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {

        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nastaveni)

        // Firebase Reference
        databazeFirebase = FirebaseDatabase.getInstance()
        referenceFirebaseUzivatel = databazeFirebase.getReference("users")

        // Aktulání uživatel Firebase
        uzivatel = FirebaseAuth.getInstance().currentUser!!

        // Tlačítko Zpět
        nastaveni_button_zpet = findViewById(R.id.nastaveni_button_zpet)

        // Přesměrování uživatele na aktivitu Účet
        nastaveni_button_zpet.setOnClickListener {

            startActivity(Intent(this, Ucet::class.java))
        }

        // Dark/White mode
        nastaveni_switch_mode = findViewById(R.id.nastaveni_switch_mode)

        // Aktivace tmavého motivu
        /*if (nightMode) {

            nastaveni_switch_mode.isChecked = true
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }*/

        // Nastavení Dark/White motivu

        nastaveni_switch_mode = findViewById(R.id.nastaveni_switch_mode)

        // Oznámení
        nastaveni_switch_oznameni = findViewById(R.id.nastaveni_switch_oznameni)

        getSwitchDB("oznameni", nastaveni_switch_oznameni)
        getSwitchDB("darkMode", nastaveni_switch_mode)

        // Kontrola zda jsou oznámení zapnuta či vypnuta
        nastaveni_switch_oznameni.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked) {

                println("Oznámení jsou zapnuta!")

                oznameniZapnuta = true

            } else {

                println("Oznámení jsou vypnuta!")

                oznameniZapnuta = false
            }

            oznameniDB(uzivatel)
            nastaveni_switch_oznameni.isChecked = oznameniZapnuta
        }


        // Kontrola zda je dark mode zapnut či vypnut
        nastaveni_switch_mode.setOnClickListener {

             if (darkModeZapnut) {

                println("Dark mode je zapnut!")

                darkModeZapnut = false

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            } else {

                println("Dark mode je vypnut!")

                darkModeZapnut = true

                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            // Mód uživatele
            darkModeDB(uzivatel)
            nastaveni_switch_mode.isChecked = darkModeZapnut
        }
    }

    private fun oznameniDB(uzivatel: FirebaseUser?) {

        if (uzivatel != null) {

            uzivatel.displayName?.let {

                referenceFirebaseUzivatel.child(it).child("oznameni").setValue(oznameniZapnuta)
            }
        }
    }

    private fun darkModeDB(uzivatel: FirebaseUser?) {

        if (uzivatel != null) {

            uzivatel.displayName?.let {

                referenceFirebaseUzivatel.child(it).child("darkMode").setValue(darkModeZapnut)
            }
        }
    }

    private fun getSwitchDB(switchNazev: String, switchCompat: SwitchCompat) {

        referenceFirebaseUzivatel.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val switchUzivatel = uzivatel.displayName?.let { snapshot.child(it).child(switchNazev).getValue(Boolean::class.java) }

                if (switchUzivatel != null) {

                    switchCompat.isChecked = switchUzivatel

                    if (switchNazev == "darkMode") {

                        darkModeZapnut = switchUzivatel
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}