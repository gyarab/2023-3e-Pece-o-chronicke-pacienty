package com.example.aplikaceprochronickpacienty.prihlaseni

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.aplikaceprochronickpacienty.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class Registrace : AppCompatActivity() {

    // Údaje uživatele
    private lateinit var registraceJmenoPrijmeni: EditText
    private lateinit var registraceEmail: EditText
    private lateinit var registraceUzivatelskeJmeno: EditText
    private lateinit var registraceHeslo: EditText

    // Zobrazit a Skrýt heslo
    private lateinit var registraceEye: ImageButton

    // Přihlášení přes odkaz
    private lateinit var registracePrihlasteSe: TextView

    // Tlačítko registrace
    private lateinit var registrace_button: Button

    // Databáze
    private lateinit var databazeFirebase: FirebaseDatabase
    private lateinit var referenceFirebase: DatabaseReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_registrace)

        registraceJmenoPrijmeni = findViewById(R.id.registrace_jmeno_a_prijmeni)
        registraceEmail = findViewById(R.id.registrace_email)
        registraceUzivatelskeJmeno = findViewById(R.id.registrace_uzivatelske_jmeno)
        registraceHeslo = findViewById(R.id.registrace_heslo)

        registraceEye = findViewById(R.id.registrace_eye_show)

        // Viditelnost při psaní hesla
        registraceEye.setOnClickListener {


            if (registraceHeslo.transformationMethod.equals(HideReturnsTransformationMethod.getInstance())) {

                registraceHeslo.transformationMethod = PasswordTransformationMethod.getInstance()

                registraceEye.setImageResource(R.drawable.ic_eye_hide)

                // nastavení kurzoru na konec věty
                registraceHeslo.setSelection(registraceHeslo.length())

            } else {

                registraceHeslo.transformationMethod = HideReturnsTransformationMethod.getInstance()

                registraceEye.setImageResource(R.drawable.ic_eye_show)

                // nastavení kurzoru na konec věty
                registraceHeslo.setSelection(registraceHeslo.length())
            }
        }

        registracePrihlasteSe = findViewById(R.id.registrace_UzMateUcetPrihlasteSe)
        registrace_button = findViewById(R.id.registrace_button)

        val checkmark = ContextCompat.getDrawable(this@Registrace, R.drawable.ic_checkmark)

        val person = ContextCompat.getDrawable(this@Registrace, R.drawable.ic_person)
        val email = ContextCompat.getDrawable(this@Registrace, R.drawable.ic_email)
        val account = ContextCompat.getDrawable(this@Registrace, R.drawable.ic_account)
        val heslo = ContextCompat.getDrawable(this@Registrace, R.drawable.ic_password)

        /** Přidání údajů do mapy **/

        var mapa: HashMap<EditText, Drawable?> = HashMap<EditText, Drawable?>()

        mapa.put(registraceJmenoPrijmeni, person)
        mapa.put(registraceEmail, email)
        mapa.put(registraceUzivatelskeJmeno, account)
        mapa.put(registraceHeslo, heslo)


        // Po kliknutí je uživatel přesměrován na Přihlášení
        registracePrihlasteSe.setOnClickListener {

            val intent = Intent(this@Registrace, Prihlaseni::class.java)
            startActivity(intent)
        }

        /** Kontrola slov - zda minimální počet slov je 2 **/
        fun checkPocetSlov(editText: EditText): Boolean {

            val vstup = editText.text.toString().trim()
            val slova = vstup.split("\\s+".toRegex())

            return slova.size >= 2
        }

        /** Kontrola emailu - zda je validní **/
        fun checkEmail(email: EditText): Boolean {

            return !TextUtils.isEmpty(email.text.toString()) && Patterns.EMAIL_ADDRESS.matcher(email.text.toString())
                .matches()
        }

        fun checkUzivatel(uzivatel: EditText): Boolean {

            return uzivatel.text.toString().isNotEmpty()
        }

        /** Kontrola hesla - zda je validní **/
        fun checkHeslo(password: EditText): Boolean {

            val heslo = password.text.toString()

            if (heslo.length < 8) return false
            if (heslo.filter { it.isDigit() }.firstOrNull() == null) return false
            if (heslo.filter { it.isLetter() }.filter { it.isUpperCase() }
                    .firstOrNull() == null) return false
            if (heslo.filter { it.isLetter() }.filter { it.isLowerCase() }
                    .firstOrNull() == null) return false
            if (heslo.filter { !it.isLetterOrDigit() }.firstOrNull() == null) return false

            return true
        }

        /** Sledování průběhu uživatele při vyplňování údajů **/
        fun addDrawableTextWatcher(
            editText: EditText,
            kontrolaFunkce: () -> Boolean,
            checkmark: Drawable?,
            ikona: Drawable?
        ) {
            editText.addTextChangedListener(object : TextWatcher {
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
                    Log.d("TEXT", editText.text.toString())

                    if (kontrolaFunkce()) {
                        editText.setCompoundDrawablesWithIntrinsicBounds(
                            ikona,
                            null,
                            checkmark,
                            null
                        )
                    } else {
                        editText.setCompoundDrawablesWithIntrinsicBounds(ikona, null, null, null)
                    }
                }
            })
        }

        for (key in mapa.keys) {

            if (key == registraceJmenoPrijmeni) {

                addDrawableTextWatcher(
                    key,
                    { checkPocetSlov(key) },
                    checkmark,
                    mapa[key]
                )

            } else if (key == registraceEmail) {

                addDrawableTextWatcher(
                    key,
                    { checkEmail(key) },
                    checkmark,
                    mapa[key]
                )

            } else if (key == registraceUzivatelskeJmeno) {

                addDrawableTextWatcher(
                    key,
                    { checkUzivatel(key) },
                    checkmark,
                    mapa[key]
                )
            } else {

                addDrawableTextWatcher(
                    key,
                    { checkHeslo(key) },
                    null,
                    mapa[key]
                )
            }
        }


        fun kontrolaChyby(editText: EditText, zprava: String): Boolean {

            val text = editText.text.toString()

            return if (text.isEmpty()) {

                editText.error = zprava
                false

            } else {

                editText.error = null
                true
            }
        }

        fun kontrolaJmenoAprijmeni(): Boolean {

            return if (!checkPocetSlov(registraceJmenoPrijmeni)) {

                registraceJmenoPrijmeni.error = "Chybí jméno nebo přijmení"
                false

            } else {
                kontrolaChyby(registraceJmenoPrijmeni, "Jméno a příjmení nemůže být prázdné")
            }
        }


        fun kontrolaEmail(): Boolean {

            return if (!checkEmail(registraceEmail)) {

                registraceEmail.error = "Tento email není validný"
                false

            } else {

                kontrolaChyby(registraceEmail, "Email nemůže být prázdný")
            }
        }

        fun kontrolaUzivatelskeJmeno(): Boolean {

            return kontrolaChyby(registraceUzivatelskeJmeno, "Uživatelské jméno nemůže být prázdné")
        }

        fun kontrolaHeslo(): Boolean {

            return if (!checkHeslo(registraceHeslo)) {

                registraceHeslo.error =
                    "Heslo musí obsahovat minimálně osm znaků, včetně písmen (velká a malá), čísel a speciálních znaků"
                false

            } else {

                kontrolaChyby(registraceHeslo, "Heslo nemůže být prázdné")
            }
        }

        registrace_button.setOnClickListener {

            if (!kontrolaJmenoAprijmeni() or !kontrolaEmail() or !kontrolaUzivatelskeJmeno() or !kontrolaHeslo()) {

                Toast.makeText(
                    this@Registrace,
                    "Prosím vyplňte chybějící údaje",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                databazeFirebase = FirebaseDatabase.getInstance()
                referenceFirebase = databazeFirebase.getReference("users")

                val jmenoPrijmeni = registraceJmenoPrijmeni.text.toString()
                val email = registraceEmail.text.toString()
                val uzivatelskeJmeno = registraceUzivatelskeJmeno.text.toString()
                val heslo = registraceHeslo.text.toString()

                val udajeUzivatele = UdajeUzivatele(jmenoPrijmeni, email, uzivatelskeJmeno, heslo)
                referenceFirebase.child(uzivatelskeJmeno).setValue(udajeUzivatele)

                Toast.makeText(
                    this@Registrace,
                    "Zaregistrovali jste se úspěšně!",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this@Registrace, Prihlaseni::class.java)
                startActivity(intent)
            }
        }
    }
}