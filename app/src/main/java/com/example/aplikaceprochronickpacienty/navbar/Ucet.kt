package com.example.aplikaceprochronickpacienty.navbar

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.prihlaseni.Prihlaseni
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth


class Ucet : AppCompatActivity() {

    private lateinit var jmenoUzivatele: TextView
    private lateinit var odhlasitButton: Button
    private lateinit var imageview: ImageView
    private lateinit var ucet_email: TextView
    private lateinit var ucet_datum_narozeni: TextView
    private lateinit var ucet_vyska: TextView
    private lateinit var ucet_vaha: TextView
    private lateinit var ucet_vek: TextView


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
        odhlasitButton = findViewById(R.id.ucet_odhlasit_button)
        imageview = findViewById(R.id.ucet_imageview)

        // Udáje uživatele
        ucet_email = findViewById(R.id.ucet_email)
        ucet_datum_narozeni = findViewById(R.id.ucet_rok_narozeni)
        ucet_vyska = findViewById(R.id.ucet_vyska)
        ucet_vaha = findViewById(R.id.ucet_vaha)
        ucet_vek = findViewById(R.id.ucet_vek)


        // Aktuální uživatel
        val uzivatel = FirebaseAuth.getInstance().currentUser

        // Jméno uživatele
        val jmeno = uzivatel?.displayName
        jmenoUzivatele.text = jmeno

        // Email uživatele
        val email = uzivatel?.email
        ucet_email.text = email


        // Kliknutí na tlačítko - Odhlásit se
        odhlasitButton.setOnClickListener {

            // Odhlášení z Firebase - Auth
            FirebaseAuth.getInstance().signOut()

            // Přesunutí na aktivitu Přihlášení
            startActivity(Intent(this , Prihlaseni::class.java))
        }
    }
}