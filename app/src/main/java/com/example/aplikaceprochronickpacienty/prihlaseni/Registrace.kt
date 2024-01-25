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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.aplikaceprochronickpacienty.R
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


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

        val mapa: HashMap<EditText, Drawable?> = HashMap<EditText, Drawable?>()

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

        /** Komtrola zda email se nachází v databázi **/
        fun emailExistuje(email: EditText): Boolean {

            val emailText = email.text.toString()

            val vysledek: Boolean = false

            val databazeReference: DatabaseReference =
                FirebaseDatabase.getInstance().getReference("users")

            databazeReference.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val emails = mutableListOf<String>()

                    for (uzivatelSnapshot in snapshot.children) {

                        val vsechnyEmaily =
                            uzivatelSnapshot.child("email").getValue(String::class.java)
                        vsechnyEmaily?.let { emails.add(it) }
                    }

                    if (emails.contains(emailText)) {

                        email.error = "Tento email je již zaregistrovaný"

                    } else {

                        email.error = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

            return vysledek
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

            when (key) {

                registraceJmenoPrijmeni -> {

                    addDrawableTextWatcher(
                        key,
                        { checkPocetSlov(key) },
                        checkmark,
                        mapa[key]
                    )

                }

                registraceEmail -> {

                    addDrawableTextWatcher(
                        key,
                        { checkEmail(key) },
                        checkmark,
                        mapa[key]
                    )

                }

                registraceUzivatelskeJmeno -> {

                    addDrawableTextWatcher(
                        key,
                        { checkUzivatel(key) },
                        checkmark,
                        mapa[key]
                    )
                }

                else -> {

                    addDrawableTextWatcher(
                        key,
                        { checkHeslo(key) },
                        null,
                        mapa[key]
                    )
                }
            }
        }

        /** Kontrola, zda text ve vstupu není prázdný **/
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

        /** Kontrola slov celého jména **/
        fun kontrolaJmenoAprijmeni(): Boolean {

            return if (!checkPocetSlov(registraceJmenoPrijmeni)) {

                registraceJmenoPrijmeni.error = "Chybí jméno nebo přijmení"
                false

            } else {
                kontrolaChyby(registraceJmenoPrijmeni, "Jméno a příjmení nemůže být prázdné")
            }
        }

        /** Kontrola emailu **/
        fun kontrolaEmail(): Boolean {

            return when {

                (registraceEmail.text.toString().isEmpty()) -> {

                    registraceEmail.error = "Email nemůže být prázdný"
                    false
                }

                !checkEmail(registraceEmail) -> {

                    registraceEmail.error = "Tento email není platný"
                    false
                }

                emailExistuje(registraceEmail) -> {

                    registraceEmail.error = "Tento email není zaregistrovaný"
                    false
                }

                else -> true
            }
        }

        /** Kontrola uživatelského jména, zda není prázdné **/
        fun kontrolaUzivatelskeJmeno(): Boolean {

            return kontrolaChyby(registraceUzivatelskeJmeno, "Uživatelské jméno nemůže být prázdné")
        }

        /** Kontrola hesla, musí obsahovat minimálně:
         *
         * - osm znaků
         * - malé a velké písmeno
         * - speciální znak (tečka, čárka, atd.)
         *
         * **/
        fun kontrolaHeslo(): Boolean {

            return when {

                !checkHeslo(registraceHeslo) -> {
                    Toast.makeText(
                        this@Registrace,
                        "Heslo musí obsahovat minimálně osm znaků, včetně písmen (velká a malá), čísel a speciálních znaků",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }

                registraceHeslo.text.isEmpty() -> {
                    Toast.makeText(
                        this@Registrace,
                        "Heslo nemůže být prázdné",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }

                else -> true
            }

        }

        /** Kliknutí na tlačítko registrace **/
        registrace_button.setOnClickListener {

            // Kontrola, jestli všechny metody jsou vyplněné správně
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

                // Vytvoření uživatele v databázi Firebase - Auth, s emailem a heslem
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, heslo)

                    .addOnCompleteListener { zprava ->

                        // Pokud je žádost úspěšná, spustí se okno přihlášení
                        if (zprava.isSuccessful) {

                            val uzivatelFirebaseAuth: FirebaseUser = zprava.result!!.user!!

                            val prihlaseni = Intent(
                                this@Registrace,
                                Prihlaseni::class.java
                            )

                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(jmenoPrijmeni)
                                .build()

                            uzivatelFirebaseAuth.updateProfile(profileUpdates)
                                .addOnCompleteListener(
                                    OnCompleteListener<Void?> { task ->

                                        Log.d("JMENO", uzivatelFirebaseAuth.displayName.toString())
                                    })


                            // Firebase Realtime database
                            val udajeUzivatele =

                                UdajeUzivatele(
                                    jmenoPrijmeni,
                                    email,
                                    uzivatelskeJmeno,
                                    false,
                                    true,
                                    true,
                                    "",
                                    "",
                                    0.0,
                                    0,
                                    0.0
                                )

                            referenceFirebase.child(jmenoPrijmeni).setValue(udajeUzivatele)

                            Toast.makeText(
                                this@Registrace,
                                "Zaregistrovali jste se úspěšně!",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(prihlaseni)

                        } else {

                            Toast.makeText(
                                this@Registrace,
                                "Došlo k chybě " + zprava.exception.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }
}