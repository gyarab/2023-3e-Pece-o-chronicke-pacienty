package com.example.aplikaceprochronickpacienty.prihlaseni

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Home
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
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
    val RC_SIGN_IN: Int = 1
    lateinit var gso: GoogleSignInOptions
    lateinit var mAuth: FirebaseAuth

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

        // Přihlášení přes Google
        prihlaseni_google_btn = findViewById(R.id.prihlaseni_pres_google_btn)

        mAuth = FirebaseAuth.getInstance()

        createRequest()

        prihlaseni_google_btn.setOnClickListener {
            signIn();
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

        prihlaseni_button.setOnClickListener {

            if (!kontrolaEmail() or !kontrolaHesla()) {

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

    /** Přihlášení přes Google **/
    private fun createRequest() {

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        prihlaseni_google_client = GoogleSignIn.getClient(this, gso);
    }

    private fun signIn() {
        val signInIntent = prihlaseni_google_client.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val exception = task.exception

            try {

                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {

                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT)
                    .show()
            }

        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        mAuth.signInWithCredential(credential)

            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val googleIntent = Intent(this, Home::class.java)

                    googleIntent.putExtra("email", account.email)
                    googleIntent.putExtra("name", account.displayName)

                    startActivity(googleIntent)

                } else {

                    Toast.makeText(this@Prihlaseni, task.exception.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    /** Kontrola emailu - zda je validní **/
    private fun checkEmail(email: EditText): Boolean {

        return !TextUtils.isEmpty(email.text.toString()) && Patterns.EMAIL_ADDRESS.matcher(email.text.toString())
            .matches()
    }

    /** Komtrola zda email se nachází v databázi **/
    private fun emailExistuje(email: EditText): Boolean {

        val emailText = email.text.toString()

        val databazeReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("users")

        databazeReference.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val emails = mutableListOf<String>()

                for (uzivatelSnapshot in snapshot.children) {

                    val vsechnyEmaily = uzivatelSnapshot.child("email").getValue(String::class.java)
                    vsechnyEmaily?.let { emails.add(it) }
                }

                if (!emails.contains(emailText)) {

                    email.error = "Tento email není zaregistrovaný"

                } else {

                    email.error = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        return false
    }

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

            emailExistuje(prihlaseniEmail) -> {

                prihlaseniEmail.error = "Tento email není zaregistrovaný"
                false
            }

            else -> true
        }
    }

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
                        Home::class.java
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