package com.example.aplikaceprochronickpacienty.navbar

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.internetPripojeni.Internet
import com.example.aplikaceprochronickpacienty.internetPripojeni.InternetPripojeni
import com.example.aplikaceprochronickpacienty.nastaveni_aplikaceInfo.AplikaceInfo
import com.example.aplikaceprochronickpacienty.nastaveni_aplikaceInfo.Nastaveni
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
import com.makeramen.roundedimageview.RoundedImageView
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class Ucet : AppCompatActivity() {

    // Nastavení
    private lateinit var ucet_nastaveni: TextView

    // O aplikaci
    private lateinit var ucet_o_aplikaci: TextView

    // Údaje uživatele
    private lateinit var ucet_jmenoPrijmeni: TextView
    private lateinit var ucet_email: TextView
    private lateinit var ucet_datum_narozeni: TextView
    private lateinit var ucet_vyska: TextView
    private lateinit var ucet_vaha: TextView
    private lateinit var ucet_vek: TextView
    private lateinit var ucet_odhlasitButton: Button

    // Odhlášení uživatele
    private lateinit var odhlaseni_google_client: GoogleSignInClient
    private lateinit var googleSignOutOptions: GoogleSignInOptions

    // Aktuální uživatel
    private var uzivatel: FirebaseUser? = null

    // Firebase Realtime database
    private lateinit var databazeFirebase: FirebaseDatabase
    private lateinit var referenceFirebaseUzivatel: DatabaseReference

    // Datum narození uživatele
    private lateinit var datumNarozeni: String

    // Výběr ikony uživatele
    private lateinit var ucet_select_icon_button: ImageView

    // Ikona uživatele
    private lateinit var ucet_icon: RoundedImageView

    @SuppressLint("MissingInflatedId", "QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_ucet)

        val pripojeni = InternetPripojeni()

        if (pripojeni.checkInternetConnection(this)) {

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

            // Výběr ikony uživatele
            ucet_select_icon_button = findViewById(R.id.ucet_select_icon_button)

            // Ikona uživatele
            ucet_icon = findViewById(R.id.ucet_icon)

            // Firebase Reference
            databazeFirebase = FirebaseDatabase.getInstance()
            referenceFirebaseUzivatel = databazeFirebase.getReference("users")

            // Celé jméno uživatele
            ucet_jmenoPrijmeni = findViewById(R.id.ucet_jmeno_uzivatele)

            // Tlačítko pro odhálšení uživatele
            ucet_odhlasitButton = findViewById(R.id.ucet_odhlasit_button)

            // Nastavení
            ucet_nastaveni = findViewById(R.id.ucet_nastaveni)

            // O aplikaci
            ucet_o_aplikaci = findViewById(R.id.ucet_o_aplikaci)

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
            getUserDataDB("profilovyObrazek")

            // Kliknutí na výběr profilového obrázku
            ucet_select_icon_button.setOnClickListener {

                val vyber = Intent(Intent.ACTION_PICK)
                vyber.type = "image/*"

                startActivityForResult(vyber, 1000)
            }

            // Kliknutí na tlačítko - Odhlásit se
            ucet_odhlasitButton.setOnClickListener {

                // Odhlášení z Firebase - Auth
                googleSignOutOptions =
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()

                // Google Client Set up
                odhlaseni_google_client = GoogleSignIn.getClient(this, googleSignOutOptions)
                odhlaseni_google_client.signOut()
                FirebaseAuth.getInstance().signOut()

                // Přesunutí na aktivitu Přihlášení
                startActivity(Intent(this, Prihlaseni::class.java))
            }

            // Přesměrování uživatele na aktivitu Nastavení
            ucet_nastaveni.setOnClickListener {

                startActivity(Intent(this, Nastaveni::class.java))
            }

            // Přesměrování uživatele na aktivitu O aplikaci
            ucet_o_aplikaci.setOnClickListener {

                startActivity(Intent(this, AplikaceInfo::class.java))
            }

        } else {

            startActivity(Intent(applicationContext, Internet::class.java))
        }
    }

    /** Metoda pro zobrazení obrázku z galerie uživatele **/
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == 1000) {

            // Kvůli chybě SecurityException je nutné použít formát Bitmap, který již není soukromý
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data?.data)

            // Přeformátování na formát Uri
            val tempUri: Uri = getImageUriFromBitmap(applicationContext, bitmap)

            // Stanovení obrázku
            ucet_icon.setImageURI(tempUri)
            ucet_icon.setScaleType(ImageView.ScaleType.FIT_XY)

            // přidání URI obrázku to databáze
            referenceFirebaseUzivatel.child(uzivatel?.displayName.toString()).child("profilovyObrazek").setValue(tempUri.toString())
        }
    }

    /** Získání formátu Uri z Bitmap
     * citace kódu: https://medium.com/bobble-engineering/java-lang-securityexception-permission-denial-opening-provider-4dca9425b448 **/
    private fun getImageUriFromBitmap(context: Context?, bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,bytes)
        val path = MediaStore.Images.Media.insertImage(context!!.contentResolver,bitmap,"File",null)
        return Uri.parse(path.toString())

    }

    private fun getUserDataDB(nazevInfo: String) {

        referenceFirebaseUzivatel.addListenerForSingleValueEvent(object : ValueEventListener {

            @SuppressLint("SetTextI18n", "DiscouragedApi", "UseCompatLoadingForDrawables")
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

                            "profilovyObrazek" -> {

                                if (ucet_icon.getDrawable() != null) {

                                    ucet_icon.setImageURI(uzivatelUdaje.toUri())
                                    ucet_icon.setScaleType(ImageView.ScaleType.FIT_XY)

                                } else {

                                    val drawable = "@drawable/ucet_ikona_account"

                                    val obrazek = getResources().getIdentifier(drawable, null,
                                        packageName
                                    )

                                    val res = getResources().getDrawable(obrazek)

                                    ucet_icon.setImageDrawable(res)
                                }

                            }
                        }

                        try {
                            ucet_vek.text = getAge(datumNarozeni).toString() + " let"

                        } catch (e:Exception) {

                            println("Tento účet nemá datum narození")
                        }

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