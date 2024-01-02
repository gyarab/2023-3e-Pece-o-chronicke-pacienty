package com.example.aplikaceprochronickpacienty.prihlaseni

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Home
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class Prihlaseni : AppCompatActivity() {

    private lateinit var prihlaseniUzivatelskeJmeno: EditText
    private lateinit var prihlaseniHeslo: EditText

    // Přihlášení přes odkaz
    private lateinit var prihlaseniZaregistrujteSe: TextView

    // Tlačítko registrace
    private lateinit var prihlaseni_button: Button

    // Zobrazit a Skrýt heslo
    private lateinit var prihlaseniEye: ImageButton

    // Zapomenutí hesla
    private lateinit var zapomenutiHesla: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_prihlaseni)

        prihlaseniUzivatelskeJmeno = findViewById(R.id.prihlaseni_uzivatelske_jmeno)
        prihlaseniHeslo = findViewById(R.id.prihlaseni_heslo)

        prihlaseni_button = findViewById(R.id.prihlaseni_button)
        prihlaseniZaregistrujteSe = findViewById(R.id.prihlaseni_nemateUcetZaregistrujteSe)

        zapomenutiHesla = findViewById(R.id.zapomenuti_hesla)

        // Po kliknutí je uživatel přesměrován na Registraci
        prihlaseniZaregistrujteSe.setOnClickListener {

            val intent = Intent(this@Prihlaseni, Registrace::class.java)
            startActivity(intent)
        }

        // Po kliknutí je uživatel přesměrován na Zapomenutí hesla
        zapomenutiHesla.setOnClickListener {

            val intent = Intent(this@Prihlaseni, ObnoveniHesla::class.java)
            startActivity(intent)
        }

        prihlaseni_button.setOnClickListener {

            if (!kontrolaUzivatelskehoJmena() or !kontrolaHesla()) {

                Toast.makeText(
                    this@Prihlaseni,
                    "Prosím vyplňte chybějící údaje",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                kontrolaUzivatele()

            }
        }

        // Viditelnost při psaní hesla

        prihlaseniEye = findViewById(R.id.registrace_eye_show)

        prihlaseniEye.setOnClickListener {

            if (prihlaseniHeslo.transformationMethod.equals(HideReturnsTransformationMethod.getInstance())) {

                prihlaseniHeslo.transformationMethod = PasswordTransformationMethod.getInstance()

                prihlaseniEye.setImageResource(R.drawable.ic_eye_hide)

                // nastavení kurzoru na konec věty
                prihlaseniHeslo.setSelection(prihlaseniHeslo.length())

            } else {

                prihlaseniHeslo.transformationMethod = HideReturnsTransformationMethod.getInstance()

                prihlaseniEye.setImageResource(R.drawable.ic_eye_show)

                // nastavení kurzoru na konec věty
                prihlaseniHeslo.setSelection(prihlaseniHeslo.length())
            }
        }
    }

    private fun kontrolaChyby(editText: EditText, zprava: String): Boolean {

        val text = editText.text.toString()

        return if (text.isEmpty()) {

            editText.error = zprava
            false

        } else {

            editText.error = null
            true
        }
    }


    private fun kontrolaUzivatelskehoJmena(): Boolean {

        return kontrolaChyby(prihlaseniUzivatelskeJmeno, "Uživatelské jméno nemůže být prázdné")
    }

    private fun kontrolaHesla(): Boolean {

        return kontrolaChyby(prihlaseniHeslo, "Heslo nemůže být prázdné")
    }

    private fun kontrolaUzivatele() {

        val uzivatel: String = prihlaseniUzivatelskeJmeno.getText().toString().trim()
        val heslo: String = prihlaseniHeslo.getText().toString().trim()

        val databazeFirebase: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("users")

        val kontrolaJmena: Query =
            databazeFirebase.orderByChild("uzivatelskeJmeno").equalTo(uzivatel)

        Log.d("KONTROLA JMENA", kontrolaJmena.toString())

        kontrolaJmena.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {

                    prihlaseniUzivatelskeJmeno.error = null

                    val hesloZfirebase =
                        snapshot.child(uzivatel).child("heslo").getValue(String::class.java)

                    Log.d("HESLO FIREBASE", hesloZfirebase.toString())
                    Log.d("HESLO MOJE", heslo)

                    Log.d("UZIVATEL", prihlaseniUzivatelskeJmeno.text.toString())


                    if (hesloZfirebase.toString() == heslo) {

                        prihlaseniUzivatelskeJmeno.error = null

                        val jmenoPrijmeniZfirebase =
                            snapshot.child(uzivatel).child("jmenoPrijmeni").getValue(
                                String::class.java
                            )
                        val emailZfirebase = snapshot.child(uzivatel).child("email").getValue(
                            String::class.java
                        )
                        val uzivatelskeJmenoZfirebase =
                            snapshot.child(uzivatel).child("uzivatelskeJmeno").getValue(
                                String::class.java
                            )

                        val intent: Intent = Intent(
                            this@Prihlaseni,
                            Home::class.java
                        )

                        intent.putExtra("jmenoPrijmeni", jmenoPrijmeniZfirebase)
                        intent.putExtra("email", emailZfirebase)
                        intent.putExtra("uzivatelskeJmeno", uzivatelskeJmenoZfirebase)
                        intent.putExtra("heslo", hesloZfirebase)

                        startActivity(intent)

                    } else {
                        prihlaseniHeslo.error = "Nesprávné heslo"

                    }

                } else {
                    prihlaseniUzivatelskeJmeno.error = "Uživatel neexistuje"
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

    }

}