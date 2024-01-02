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

class ObnoveniHesla : AppCompatActivity() {

    private lateinit var email:EditText

    private var emailText: String = ""

    private lateinit var button_reset: Button

    private lateinit var prihlaseniZaregistrujteSe:TextView

    private lateinit var firebaseAuth: FirebaseAuth
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_zapomenuti_hesla)

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

        fun kontrolaEmail(): Boolean {

            emailText = email.text.toString()

            return when {
                !checkEmail(email) -> {

                    email.error = "Tento email není platný"
                    false
                }

                emailText.isEmpty() -> {

                    email.error = "Email nemůže být prázdný"
                    false
                }

                else -> true
            }
        }

        button_reset.setOnClickListener {

            if (!kontrolaEmail()) {

                Toast.makeText(
                    this@ObnoveniHesla,
                    "Prosím vyplňte email",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                // FirebaseAuth - ověření
                firebaseAuth = FirebaseAuth.getInstance()

                Log.d("EMAIL", emailText)

                firebaseAuth.sendPasswordResetEmail(emailText).addOnSuccessListener {

                    Toast.makeText(
                        this@ObnoveniHesla,
                        "Zpráva s odkazem pro obnovení hesla byla právě odeslána na váš email",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@ObnoveniHesla, Prihlaseni::class.java)
                    startActivity(intent)

                }

                firebaseAuth.sendPasswordResetEmail(emailText).addOnCanceledListener {

                    Toast.makeText(
                        this@ObnoveniHesla,
                        "Při posílání zprávy na váš email došlo k chybě",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }
}