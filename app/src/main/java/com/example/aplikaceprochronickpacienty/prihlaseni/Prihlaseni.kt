package com.example.aplikaceprochronickpacienty.prihlaseni

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Prehled
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class Prihlaseni : AppCompatActivity() {


    private lateinit var prihlaseniEmail: EditText
    private lateinit var prihlaseniHeslo: EditText

    // Přesměrování přes odkaz
    private lateinit var prihlaseniZaregistrujteSe: TextView

    // Tlačítko registrace
    private lateinit var prihlaseni_button: Button

    // Zobrazit a Skrýt heslo
    private lateinit var prihlaseniEye: ImageButton

    // Zapomenutí hesla
    private lateinit var zapomenutiHesla: TextView

    private lateinit var prihlaseni_google_btn: Button
    private lateinit var prihlaseni_google_client: GoogleSignInClient
    private val RC_SIGN_IN: Int = 1
    private lateinit var googleSignInOptions: GoogleSignInOptions
    private var googleSignIn = false
    private var emails: MutableList<String> = mutableListOf()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_prihlaseni)

        prihlaseniEmail = findViewById(R.id.prihlaseni_email)
        prihlaseniHeslo = findViewById(R.id.prihlaseni_heslo)
        prihlaseni_button = findViewById(R.id.prihlaseni_button)
        prihlaseniZaregistrujteSe = findViewById(R.id.prihlaseni_nemateUcetZaregistrujteSe)
        zapomenutiHesla = findViewById(R.id.zapomenuti_hesla)

        prihlaseniUzivatele()

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

    /** Hlavní funkce pro přihlášení uživatele **/
    private fun prihlaseniUzivatele() {

        var prihlasit = false

        val databazeFirebase = FirebaseDatabase.getInstance()
        val referenceFirebase = databazeFirebase.getReference("users")

        referenceFirebase.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                for (uzivatelSnapshot in snapshot.children) {

                    val vsechnyEmaily = uzivatelSnapshot.child("emaily").getValue(String::class.java)
                    vsechnyEmaily?.let { emails.add(it) }
                }

            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        val uzivatel = FirebaseAuth.getInstance().currentUser

        if (uzivatel != null) {

            // Uživatel je přihlášen
            val intent = Intent(this@Prihlaseni, Prehled::class.java)
            startActivity(intent)

        } else {

            /** Uživatel není přihlášen **/

            // Výchozí přihlášení
            prihlaseni_button.setOnClickListener {

                if (!kontrolaEmail() or !kontrolaHesla()) {

                    return@setOnClickListener

                } else {
                    kontrolaUzivatele()

                }
            }

            // Přihlášení přes Google
            prihlaseni_google_btn = findViewById(R.id.prihlaseni_pres_google_btn)

            prihlaseni_google_btn.setOnClickListener {

                vytvoreniZadostiGoogle()

                googleSignIn = true
                prihlaseniIntent();
            }

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
        }
    }

    /** Přihlášení přes Google **/
    private fun vytvoreniZadostiGoogle() {

        // Nastavení přihlášení
        googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Google Client Set up
        prihlaseni_google_client = GoogleSignIn.getClient(this, googleSignInOptions)
    }

    /** Vytvoření okna pro přihlášení přes Google **/
    private fun prihlaseniIntent() {
        val signInIntent = prihlaseni_google_client.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    /** Pokus o přihlášení přes Google **/
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(zadost: Int, vysledek: Int, data: Intent?) {

        super.onActivityResult(zadost, vysledek, data)

        if (zadost == RC_SIGN_IN) {

            val zadost = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {

                val account = zadost.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)

            } catch (e: ApiException) {

                Toast.makeText(this, "Přihlášení se nezdařilo", Toast.LENGTH_SHORT)
                    .show()
            }

        }
    }

    /** Autentizace uživatele přes Firebase **/
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        if (emails.contains(account.email)) {

            Toast.makeText(
                this@Prihlaseni,
                "Tento email je již zaregistrovaný",
                Toast.LENGTH_SHORT
            )
                .show()

            Log.d("EMAILY",emails.toString())

            prihlaseni_google_client.signOut()

            return

        } else {

            FirebaseAuth.getInstance().signInWithCredential(credential)

                .addOnCompleteListener(this) { zprava ->

                    if (zprava.isSuccessful) {

                        // Realtime Firebase
                        pridatUzivatele_Realtime(account)

                        // Google Auth - pokračování
                        val googleIntent = Intent(this, Prehled::class.java)
                        startActivity(googleIntent)


                    } else {

                        Toast.makeText(
                            this@Prihlaseni,
                            zprava.exception.toString(),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
        }
    }

    /** Přidání uživatele do Firebase - Realtime database  **/
    private fun pridatUzivatele_Realtime(account: GoogleSignInAccount) {

        val databazeFirebase = FirebaseDatabase.getInstance()
        val referenceFirebase = databazeFirebase.getReference("users")

        val udajeUzivatele = UdajeUzivatele(account.displayName, account.email, account.givenName)

        // V databazi Firebase Realtime se vytvori novy uzivatel se udaji
        account.givenName?.let { referenceFirebase.child(it).setValue(udajeUzivatele) }
    }

    /** Kontrola emailu - zda je validní **/
    private fun checkEmail(email: EditText): Boolean {

        return !TextUtils.isEmpty(email.text.toString()) && Patterns.EMAIL_ADDRESS.matcher(email.text.toString())
            .matches()
    }

    /** Kontrola zda email se nachází v databázi **/
    private fun emailNeexistuje(email: String, editText: EditText?): Boolean {

        Log.d("EMAILY", emails.toString())

        if (!emails.contains(email)) {

            if (editText != null) {
                editText.error = "Tento email není zaregistrovaný"
            }

        } else if (emails.contains(email) && googleSignIn) {

            Toast.makeText(
                this@Prihlaseni,
                "Tento email je již zaregistrovaný",
                Toast.LENGTH_SHORT
            )
                .show()

            prihlaseni_google_client.signOut()

            googleSignIn = false
        }

        return false
    }

    /** Finální kontrola emailu uživatele **/
    private fun kontrolaEmail(): Boolean {

        return when {

            (prihlaseniEmail.text.toString().isEmpty()) -> {

                prihlaseniEmail.error = "Email nemůže být prázdný"
                false
            }

            !checkEmail(prihlaseniEmail) -> {

                prihlaseniEmail.error = "Tento email není platný"
                false
            }

            emailNeexistuje(prihlaseniEmail.text.toString(), prihlaseniEmail) -> {

                false
            }

            else -> true
        }
    }

    /** Finální kontrola hesla uživatele **/
    private fun kontrolaHesla(): Boolean {

        return if (prihlaseniHeslo.text.toString().isEmpty()) {

            Toast.makeText(
                this@Prihlaseni,
                "Heslo nemůže být prázdné",
                Toast.LENGTH_SHORT,
            ).show()

            false

        } else {

            true
        }
    }

    /** Celková kontrola uživatele **/
    private fun kontrolaUzivatele() {

        val uzivatel: String = prihlaseniEmail.getText().toString().trim()
        val heslo: String = prihlaseniHeslo.getText().toString().trim()

        //val databazeFirebase: DatabaseReference =
        //    FirebaseDatabase.getInstance().getReference("users")

        //val kontrolaJmena: Query =
        //    databazeFirebase.orderByChild("uzivatelskeJmeno").equalTo(uzivatel)

        //Log.d("KONTROLA JMENA", kontrolaJmena.toString())

        FirebaseAuth.getInstance().signInWithEmailAndPassword(uzivatel, heslo)

            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    Toast.makeText(
                        this@Prihlaseni,
                        "Přihlášení proběhlo úspěšně",
                        Toast.LENGTH_SHORT,
                    ).show()

                    val intent = Intent(
                        this@Prihlaseni,
                        Prehled::class.java
                    )

                    startActivity(intent)

                } else {

                    Toast.makeText(
                        this@Prihlaseni,
                        "Nesprávné heslo",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

        /*kontrolaJmena.addListenerForSingleValueEvent(object : ValueEventListener {

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
        })*/

    }

}