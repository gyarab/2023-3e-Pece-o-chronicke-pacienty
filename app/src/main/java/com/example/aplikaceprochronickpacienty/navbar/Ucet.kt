package com.example.aplikaceprochronickpacienty.navbar

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.prihlaseni.Prihlaseni
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Ucet : AppCompatActivity() {

    private lateinit var jmenoUzivatele: TextView
    private lateinit var odhlasitButton: Button
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_ucet)

        val navView: BottomNavigationView = findViewById(R.id.bottom_nav)

        navView.selectedItemId = R.id.navigation_settings

        navView.setOnNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.navigation_home -> {

                    startActivity(Intent(applicationContext, Prehled::class.java))
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigation_chat -> {
                    startActivity(Intent(applicationContext, Chat::class.java))
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigation_settings -> {
                    return@setOnNavigationItemSelectedListener true
                }

                else -> return@setOnNavigationItemSelectedListener false
            }
        }

        jmenoUzivatele = findViewById(R.id.ucet_jmeno_uzivatele)
        odhlasitButton = findViewById(R.id.ucet_odhlasit_se_button)

        // Aktuální uživatel
        val uzivatel = FirebaseAuth.getInstance().currentUser

        // Email uživatele
        val email = uzivatel?.displayName

        jmenoUzivatele.text = email

        // Kliknutí na tlačítko - Odhlásit se
        odhlasitButton.setOnClickListener {

            // Odhlášení z Firebase - Auth
            FirebaseAuth.getInstance().signOut()

            // Přesunutí na aktivitu Přihlášení
            startActivity(Intent(this , Prihlaseni::class.java))
        }
    }
}