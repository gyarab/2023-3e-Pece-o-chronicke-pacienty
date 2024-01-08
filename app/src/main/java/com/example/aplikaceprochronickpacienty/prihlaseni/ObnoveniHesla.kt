package com.example.aplikaceprochronickpacienty.prihlaseni

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.aplikaceprochronickpacienty.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ObnoveniHesla : AppCompatActivity() {

    private lateinit var email:EditText

    private var emailText: String = ""

    private lateinit var button_reset: Button

    private lateinit var prihlaseniZaregistrujteSe:TextView

    private var mapa: HashMap<String, Boolean> = HashMap()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_obnoveni_hesla)

        email = findViewById(R.id.reset_email)
        button_reset = findViewById(R.id.reset_button)

        val checkmark_ikona = ContextCompat.getDrawable(this@ObnoveniHesla, R.drawable.ic_checkmark)
        val email_ikona = ContextCompat.getDrawable(this@ObnoveniHesla, R.drawable.ic_email)

        prihlaseniZaregistrujteSe = findViewById(R.id.prihlaseni_nemateUcetZaregistrujteSe)

        // Po kliknutí je uživatel přesměrován na Registraci
        prihlaseniZaregistrujteSe.setOnClickListener {

            val intent = Intent(this@ObnoveniHesla, Registrace::class.java)
            startActivity(intent)
        }

        /** Kontrola emailu - zda je validní **/
        fun checkEmail(email: EditText): Boolean {

            return !TextUtils.isEmpty(email.text.toString()) && Patterns.EMAIL_ADDRESS.matcher(email.text.toString())
                .matches()
        }

        email.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(editable: Editable?) {

                if (checkEmail(email)) {

                    email.setCompoundDrawablesWithIntrinsicBounds(

                        email_ikona,
                        null,
                        checkmark_ikona,
                        null
                    )

                } else {
                    email.setCompoundDrawablesWithIntrinsicBounds(email_ikona, null, null, null)
                }
            }
        })

        val databazeFirebase = FirebaseDatabase.getInstance()
        val referenceFirebase = databazeFirebase.getReference("users")

        // Získání typu emailu uživatele
        referenceFirebase.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                for (uzivatelSnapshot in snapshot.children) {

                    val emaily = uzivatelSnapshot.child("email").getValue(String::class.java)
                    val googleUcet = uzivatelSnapshot.child("googleUcet").getValue(Boolean::class.java)

                    if (emaily != null && googleUcet != null) {

                        mapa.put(emaily,googleUcet)
                        Log.d("MAPA",mapa.toString())
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        fun kontrolaEmail(): Boolean {

            emailText = email.text.toString()

            val ucetGoogle = mapa.get(emailText)

            return when {
                !checkEmail(email) -> {

                    email.error = "Tento email není platný"
                    false
                }

                emailText.isEmpty() -> {

                    email.error = "Email nemůže být prázdný"
                    false
                }

                (!mapa.containsKey(emailText) && ucetGoogle == false) -> {

                    email.error = "Tento email není zaregistrovaný"
                    false
                }

                (mapa.containsKey(emailText) && ucetGoogle == true) -> {

                    email.error = "Tento typ emailu je Google"
                    false
                }

                else -> true
            }
        }

        button_reset.setOnClickListener {

            if (!kontrolaEmail()) {

                Toast.makeText(
                    this@ObnoveniHesla,
                    "Někde nastala chyba",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                // FirebaseAuth - ověření
                Log.d("EMAIL", emailText)

                FirebaseAuth.getInstance().sendPasswordResetEmail(emailText).addOnSuccessListener {

                    Toast.makeText(
                        this@ObnoveniHesla,
                        "Zpráva s odkazem pro obnovení hesla byla právě odeslána na váš email",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@ObnoveniHesla, Prihlaseni::class.java)
                    startActivity(intent)

                }
            }
        }

    }
}