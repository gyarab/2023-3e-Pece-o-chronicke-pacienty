package com.example.aplikaceprochronickpacienty.nastaveni_aplikaceInfo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.internetPripojeni.Internet
import com.example.aplikaceprochronickpacienty.internetPripojeni.InternetPripojeni
import com.example.aplikaceprochronickpacienty.navbar.Prehled
import com.example.aplikaceprochronickpacienty.navbar.Ucet
import com.example.aplikaceprochronickpacienty.roomDB.UzivatelDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class Nastaveni : AppCompatActivity() {

    // Tlačítko - zpětné vrácení
    private lateinit var nastaveni_button_zpet: ImageButton

    // Switch theme
    private lateinit var nastaveni_switch_mode: SwitchCompat
    var darkModeZapnut: Boolean = true

    // Switch oznámení
    private lateinit var nastaveni_switch_oznameni: SwitchCompat
    var oznameniZapnuta: Boolean = true

    // Switch oznámení
    private lateinit var nastaveni_kroky_editext: EditText
    private lateinit var nastaveni_vaha_editext: EditText
    private lateinit var nastaveni_datum_narozeni_udaje_textview: TextView
    private lateinit var nastaveni_vyska_udaje_editext: EditText
    private lateinit var nastaveni_vaha_udaje_editext: EditText

    // Firebase Realtime database
    private lateinit var databazeFirebase: FirebaseDatabase
    private lateinit var referenceFirebaseUzivatel: DatabaseReference

    // ROOM
    val roomDatabase = UzivatelDatabase.getDatabase(this)

    // Aktivní uživatel
    private val aktivniUzivatel = 1648

    // Uživatel Firebase Auth
    private lateinit var uzivatel: FirebaseUser

    // Uložit údaje
    private lateinit var button_ulozit: Button

    // Výběr nemocí
    private lateinit var nastaveni_vyber_nemoci: AutoCompleteTextView

    // Údaje uživatele
    private val udaje = listOf("krokyCil", "vahaCil", "datumNarozeni", "vyska", "vaha", "nemoc")

    // Aktuální nemoc
    private var nemoc: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nastaveni)

        // Kontrola připojení
        val pripojeni = InternetPripojeni()

        if (pripojeni.checkInternetConnection(this)) {

            // Chronické onemocnění
            nastaveni_vyber_nemoci = findViewById(R.id.autoCompleteTextView)

            // Výběr nemocí
            val nemoci = resources.getStringArray(R.array.nastaveni_vyber_nemoci)
            val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, nemoci)

            nastaveni_vyber_nemoci.setAdapter(arrayAdapter)

            // Získání aktuální nemoci uživatele
            nastaveni_vyber_nemoci.setOnItemClickListener { parent, view, position, id ->

                nemoc = parent.getItemAtPosition(position).toString()
            }

            // Firebase Reference
            databazeFirebase = FirebaseDatabase.getInstance()
            referenceFirebaseUzivatel = databazeFirebase.getReference("users")

            // Aktulání uživatel Firebase
            uzivatel = FirebaseAuth.getInstance().currentUser!!

            // Tlačítko Zpět
            nastaveni_button_zpet = findViewById(R.id.nastaveni_button_zpet)

            // Přesměrování uživatele na aktivitu Účet
            nastaveni_button_zpet.setOnClickListener {

                startActivity(Intent(this, Ucet::class.java))
            }

            // Dark/White mode
            nastaveni_switch_mode = findViewById(R.id.nastaveni_switch_mode)

            // Oznámení
            nastaveni_switch_oznameni = findViewById(R.id.nastaveni_switch_oznameni)

            // Získání aktuální pooložky z databáze
            getSwitchDB("oznameni", nastaveni_switch_oznameni)
            getSwitchDB("darkMode", nastaveni_switch_mode)

            // Získání dat uživatele z databáze
            for (i in udaje) {

                getUserDataDB(i)
            }

            // Kontrola zda jsou oznámení zapnuta či vypnuta
            nastaveni_switch_oznameni.setOnCheckedChangeListener { buttonView, isChecked ->

                if (isChecked) {

                    println("Oznámení jsou zapnuta!")

                    oznameniZapnuta = true

                } else {

                    println("Oznámení jsou vypnuta!")

                    oznameniZapnuta = false
                }

                oznameniDB(uzivatel)
                nastaveni_switch_oznameni.isChecked = oznameniZapnuta
            }


            // Kontrola zda je dark mode zapnut či vypnut
            nastaveni_switch_mode.setOnClickListener {

                if (darkModeZapnut) {

                    println("Dark mode je zapnut!")

                    darkModeZapnut = false

                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                } else {

                    println("Dark mode je vypnut!")

                    darkModeZapnut = true

                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }

                // Zvolený motiv (mode) uživatele
                darkModeDB(uzivatel)
                nastaveni_switch_mode.isChecked = darkModeZapnut
            }

            // Nastavení číselných paramterů u elementu EditText
            nastaveni_kroky_editext = findViewById(R.id.nastaveni_kroky_editext)
            nastaveni_vaha_editext = findViewById(R.id.nastaveni_vaha_editext)
            nastaveni_vyska_udaje_editext = findViewById(R.id.nastaveni_vyska_udaje_editext)
            nastaveni_vaha_udaje_editext = findViewById(R.id.nastaveni_vaha_udaje_editext)

            // Uložení dat
            button_ulozit = findViewById(R.id.nastaveni_ulozit_button)

            // Vybrání data narození z kalendáře Button
            nastaveni_datum_narozeni_udaje_textview =
                findViewById(R.id.nastaveni_datum_narozeni_udaje_textview)

            // Vybrání data narození uživatele
            setBithdayUser(nastaveni_datum_narozeni_udaje_textview)

            // Přidání dat uživatele
            addDataToDB()

        } else {

            startActivity(Intent(applicationContext, Internet::class.java))
        }
    }

    private fun oznameniDB(uzivatel: FirebaseUser?) {

        if (uzivatel != null) {

            uzivatel.displayName?.let {

                referenceFirebaseUzivatel.child(it).child("oznameni").setValue(oznameniZapnuta)
            }
        }
    }

    private fun darkModeDB(uzivatel: FirebaseUser?) {

        if (uzivatel != null) {

            uzivatel.displayName?.let {

                referenceFirebaseUzivatel.child(it).child("darkMode").setValue(darkModeZapnut)
            }
        }
    }

    private fun getSwitchDB(switchNazev: String, switchCompat: SwitchCompat) {

        referenceFirebaseUzivatel.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val switchUzivatel = uzivatel.displayName?.let {
                    snapshot.child(it).child(switchNazev).getValue(Boolean::class.java)
                }

                if (switchUzivatel != null) {

                    switchCompat.isChecked = switchUzivatel

                    if (switchNazev == "darkMode") {

                        darkModeZapnut = switchUzivatel
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun getUserDataDB(nazevInfo: String) {

        runBlocking {

            nastaveni_vaha_udaje_editext = findViewById(R.id.nastaveni_vaha_udaje_editext)

            val vaha = roomDatabase.uzivatelDao().getWeight(aktivniUzivatel)

            println("VAHA je $vaha")
            nastaveni_vaha_udaje_editext.hint = vaha.toString()
        }

        referenceFirebaseUzivatel.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val uzivatelUdaje = uzivatel.displayName?.let {
                    snapshot.child(it).child(nazevInfo).getValue(Any::class.java).toString()
                }

                if (uzivatelUdaje != null) {

                    if (uzivatelUdaje.toString().isNotEmpty() && uzivatelUdaje.toString() != "0") {

                        when (nazevInfo) {

                            "krokyCil" -> {

                                nastaveni_kroky_editext.hint = uzivatelUdaje

                            }

                            "vahaCil" -> {

                                nastaveni_vaha_editext.hint = uzivatelUdaje

                            }

                            "datumNarozeni" -> {

                                nastaveni_datum_narozeni_udaje_textview.text = uzivatelUdaje

                            }

                            "vyska" -> {

                                nastaveni_vyska_udaje_editext.hint = uzivatelUdaje

                            }

                            "nemoc" -> {

                                nastaveni_vyber_nemoci.setText(uzivatelUdaje, false)

                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun setBithdayUser(textView: TextView) {

        textView.setOnClickListener {

            val kalendar = Calendar.getInstance()

            // Výchozí rok
            kalendar.add(Calendar.YEAR, -10)

            val rok = kalendar.get(Calendar.YEAR)
            val mesic = kalendar.get(Calendar.MONTH)
            val den = kalendar.get(Calendar.DAY_OF_MONTH)

            // Výběr data z kalendáře
            val datePickerDialog = DatePickerDialog(

                this,
                { view, celkovyRok, mesicRoku, denMesice ->

                    val vybranyDen: String

                    if (denMesice < 10 && mesicRoku < 10) {

                        vybranyDen =
                            ("0" + denMesice.toString() + "." + "0" + (mesicRoku + 1) + "." + celkovyRok)

                    } else if (denMesice < 10) {

                        vybranyDen =
                            ("0" + denMesice.toString() + "." + (mesicRoku + 1) + "." + celkovyRok)
                    } else if (mesicRoku < 10) {

                        vybranyDen =
                            (denMesice.toString() + "." + "0" + (mesicRoku + 1) + "." + celkovyRok)
                    } else {

                        vybranyDen =
                            (denMesice.toString() + "." + (mesicRoku + 1) + "." + celkovyRok)
                    }

                    textView.text = vybranyDen

                },

                rok,
                mesic,
                den
            )

            // Minimální věk uživatele
            val maxDen = 1;
            val maxMesic = 1;
            val maxRok = 2014;
            kalendar.set(maxRok, maxMesic - 1, maxDen);

            datePickerDialog.datePicker.maxDate = kalendar.timeInMillis
            datePickerDialog.show()
        }
    }

    /** Při stisknutí tlačítka se uloží veškerá uživatelská data **/
    private fun addDataToDB() {

        button_ulozit.setOnClickListener {

            val krokyCil = nastaveni_kroky_editext.text.toString()
            val vahaCil = nastaveni_vaha_editext.text.toString()
            val datumNarozeni = nastaveni_datum_narozeni_udaje_textview.text.toString()
            val vyska = nastaveni_vyska_udaje_editext.text.toString()
            val nemoc = nastaveni_vyber_nemoci.text.toString()

            val datum = Prehled().dnesniDatum()

            println(nastaveni_vaha_udaje_editext.text)

            // Obnovení váhy v DB ROOM
            runBlocking {

                try {

                    roomDatabase.uzivatelDao()
                        .updateWeight(
                            aktivniUzivatel,
                            datum,
                            nastaveni_vaha_udaje_editext.text.toString().toDouble()
                        )

                } catch (e: NumberFormatException) {

                    println("Váha nebyla změněna")
                }
            }

            // Přidání informací do komponenty HashMap
            val mapa = hashMapOf<String, String>()
            mapa["krokyCil"] = krokyCil
            mapa["vahaCil"] = vahaCil
            mapa["datumNarozeni"] = datumNarozeni
            mapa["vyska"] = vyska
            mapa["nemoc"] = nemoc

            // Přidání nových dat uživatele
            uzivatel.displayName?.let {

                mapa.forEach { (key, value) ->

                    if (value.isNotEmpty()) {

                        addUserInfo(it, key, value)
                    }
                }

                Toast.makeText(
                    this@Nastaveni,
                    "Úspěšně uloženo!",
                    Toast.LENGTH_SHORT
                )
                    .show()

            }
        }
    }

    /** Metoda pro přidání dat uživatele **/
    private fun addUserInfo(it: String, nazev: String, info: String) {

        referenceFirebaseUzivatel.child(it).child(nazev).setValue(info)
    }

}