package com.example.aplikaceprochronickpacienty.navbar

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.nastaveni.Nastaveni
import com.example.aplikaceprochronickpacienty.prihlaseni.Prihlaseni
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class Ucet : AppCompatActivity() {

    // Nastavení
    private lateinit var ucet_nastaveni: TextView

    // Údaje uživatele
    private lateinit var ucet_jmenoPrijmeni: TextView
    private lateinit var ucet_email: TextView
    private lateinit var ucet_datum_narozeni: TextView
    private lateinit var ucet_vyska: TextView
    private lateinit var ucet_vaha: TextView
    private lateinit var ucet_vek: TextView
    private lateinit var ucet_ikonaUzivatele: ImageView
    private lateinit var ucet_odhlasitButton: Button

    // Odhlášení uživatele
    private lateinit var odhlaseni_google_client: GoogleSignInClient
    private lateinit var googleSignOutOptions: GoogleSignInOptions

    // Aktuální uživatel
    private var uzivatel: FirebaseUser? = null

    // Firebase Realtime database
    private lateinit var databazeFirebase: FirebaseDatabase
    private lateinit var referenceFirebaseUzivatel: DatabaseReference

    private lateinit var datumNarozeni: String

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

        // Firebase Reference
        databazeFirebase = FirebaseDatabase.getInstance()
        referenceFirebaseUzivatel = databazeFirebase.getReference("users")

        // Celé jméno uživatele
        ucet_jmenoPrijmeni = findViewById(R.id.ucet_jmeno_uzivatele)

        // Tlačítko pro odhálšení uživatele
        ucet_odhlasitButton = findViewById(R.id.ucet_odhlasit_button)

        // Ikona uživatele
        ucet_ikonaUzivatele = findViewById(R.id.ucet_imageview)

        // Nastavení
        ucet_nastaveni = findViewById(R.id.ucet_nastaveni)

        // Udáje uživatele
        ucet_email = findViewById(R.id.ucet_email)
        ucet_datum_narozeni = findViewById(R.id.ucet_rok_narozeni)
        ucet_vyska = findViewById(R.id.ucet_vyska)
        ucet_vaha = findViewById(R.id.ucet_vaha)
        ucet_vek = findViewById(R.id.ucet_vek)


        // Aktuální uživatel
        uzivatel = FirebaseAuth.getInstance().currentUser

        // Jméno uživatele
        val jmeno = uzivatel?.displayName
        ucet_jmenoPrijmeni.text = jmeno

        println(jmeno)

        // Email uživatele
        val email = uzivatel?.email
        ucet_email.text = email

        // Ostatní údaje uživatele
        getUserDataDB("datumNarozeni")
        getUserDataDB("vyska")
        getUserDataDB("vaha")
        getUserDataDB("vek")

        // Kliknutí na tlačítko - Odhlásit se
        ucet_odhlasitButton.setOnClickListener {

            // Odhlášení z Firebase - Auth
            googleSignOutOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            // Google Client Set up
            odhlaseni_google_client = GoogleSignIn.getClient(this, googleSignOutOptions)
            odhlaseni_google_client.signOut()
            FirebaseAuth.getInstance().signOut()

            // Přesunutí na aktivitu Přihlášení
            startActivity(Intent(this , Prihlaseni::class.java))
        }

        // Přesměrování uživatele na aktivitu Nastavení
        ucet_nastaveni.setOnClickListener {

            startActivity(Intent(this,Nastaveni::class.java))
        }

        println(getAge("01.03.2006"))

    }

    private fun getUserDataDB(nazevInfo: String) {

        referenceFirebaseUzivatel.addListenerForSingleValueEvent(object : ValueEventListener {

            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {

                val uzivatelUdaje = uzivatel?.displayName?.let {
                    snapshot.child(it).child(nazevInfo).getValue(Any::class.java).toString()
                }

                if (uzivatelUdaje != null) {

                    if (uzivatelUdaje.toString().isNotEmpty() && uzivatelUdaje.toString() != "0") {

                        when (nazevInfo) {

                            "datumNarozeni" -> {

                                ucet_datum_narozeni.text = uzivatelUdaje
                                datumNarozeni = uzivatelUdaje
                            }

                            "vyska" -> {

                                ucet_vyska.text = "$uzivatelUdaje cm"

                            }

                            "vaha" -> {

                                ucet_vaha.text = "$uzivatelUdaje kg"

                            }
                        }

                        ucet_vek.text = getAge(datumNarozeni).toString() + " let"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    /** Získání věku podle data narození uživatele **/
    fun getAge(dateString: String): Int {

        val format = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        val datumNarozeni = LocalDate.parse(dateString, format)

        val dnesniDatum = LocalDate.now()

        val vek = ChronoUnit.YEARS.between(datumNarozeni, dnesniDatum).toInt()

        return vek
    }

}