package com.example.aplikaceprochronickpacienty.nastaveni_aplikaceInfo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Ucet


class AplikaceInfo : AppCompatActivity() {

    // Tlačítko zpět do scény Účet
    private lateinit var aplikace_button_zpet: ImageButton

    // Popis aplikace
    private lateinit var aplikace_listview: ListView

    // Zobrazení PDF
    private lateinit var aplikace_pdfviewer_button: Button

    // Názvy u obsahu
    private var nazvyData = ArrayList<String>()

    // Text u obsahu
    private var textData = ArrayList<String>()

    // Počet
    var pocet = 0

    // Kontrola Dark/White mode
    var darkMode = false

    // Odkaz na weové stránky Gyarab
    private lateinit var aplikace_gyarab_imageview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_aplikace_info)

        // Logo Gyarab
        aplikace_gyarab_imageview = findViewById(R.id.aplikace_gyarab_imageview)

        aplikace_gyarab_imageview.setOnClickListener {

            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gyarab.cz/")))
        }

        // Tlačítko zpět do scény Účet
        aplikace_button_zpet = findViewById(R.id.aplikace_button_zpet)

        // Popis aplikace
        aplikace_listview = findViewById(R.id.aplikace_listview)

        // Přidání popisu
        addDescription(aplikace_listview)

        // Zobrazení dokumentu PDF
        aplikace_pdfviewer_button = findViewById(R.id.aplikace_pdfviewer_button)

        // Přesměrování uživatele na aktivitu Zobrazení PDF
        aplikace_pdfviewer_button.setOnClickListener {

            startActivity(Intent(this, ZobrazeniPDF::class.java))
        }

        // Zpětné přesměrování uživatele na aktivitu Účet
        aplikace_button_zpet.setOnClickListener {

            startActivity(Intent(this, Ucet::class.java))
        }
    }

    /** Popis aplikace **/
    private fun addDescription(listView: ListView) {

        val data = mutableListOf(

            mapOf(
                "nadpis" to " — Téma: ",
                "text" to "\n Mobilní aplikace pro chronicky nemocné pacienty \n"
            ),
            mapOf("nadpis" to " — Autoři: ", "text" to "\n - Vladimír Samojlov (Student) \n - Felix Navrátil (Student) \n - Kryštof Breburda (Student) \n"),
            mapOf("nadpis" to " — Organizace: ", "text" to "\n Gymnázium, Praha 6, Arabská 14 \n"),
            mapOf(
                "nadpis" to " — Zdrojový kód: ",
                "text" to "\n Link na Github \n"
            ),
            mapOf("nadpis" to " — Kontakt: ", "text" to "\n " +

                "vladimir.samojlov@student.gyarab.cz \n" +
                "\n felix.navratil@student.gyarab.cz \n" +
                "\n krystof.breburda@student.gyarab.cz \n")

        )

        // Informace s hodnotami map - hodnota u nadpisu, hodnota u textu
        val informace = data.flatMap { it.values }

        // Z mapy rozdělíme hodnoty do dvou listů
        for (i in informace) {

            if (pocet % 2 == 0) {

                // Přidání informací do listu názvy
                nazvyData.add(i)

            } else {

                // Přidání informací do listu text
                textData.add(i)
            }

            pocet++
        }

        // Aktuální motiv aplikace
        isDarkMode()

        val adapter = ListAdapter(
            this,
            data,
            android.R.layout.simple_list_item_2,
            arrayOf("nadpis", "text"),
            intArrayOf(android.R.id.text1, android.R.id.text2),
            nazvyData,
            textData,
            darkMode
        )

        listView.adapter = adapter

        // Při kliknutí na poslední dva elementy proběhne přesměrování
        listView.setOnItemClickListener { _, _, pozice, _ ->

            when (pozice) {

                3 -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gyarab/2023-3e-Pece-o-chronicke-pacienty")))
                }

                4 -> {

                    poslatEmail("vladimir.samojlov@student.gyarab.cz", "Dotaz ohledně ...")
                }
            }
        }
    }

    /** Zaslání emailu na adresu vývojáře **/
    @SuppressLint("QueryPermissionsNeeded")
    fun poslatEmail(email: String?, subject: String?) {

        val emailIntent = Intent(
            Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", email, null
            )
        )
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        //emailIntent.putExtra(Intent.EXTRA_TEXT, "Body")
        startActivity(Intent.createChooser(emailIntent, "Píšu email ohledně..."))
    }

    /** Vypsání aktuálního motivu aplikace **/
    private fun isDarkMode(): Boolean {

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        when (currentNightMode) {

            Configuration.UI_MODE_NIGHT_NO -> {

                darkMode = false
            }
            Configuration.UI_MODE_NIGHT_YES -> {

                darkMode = true
            }
        }

        return darkMode
    }

    /** Adapter pro komponent ListView **/
    private class ListAdapter(
        context: Context,
        private val data: List<Map<String, String>>,
        resource: Int,
        from: Array<String>,
        to: IntArray,
        private val nazvyData: ArrayList<String>,
        private val textData: ArrayList<String>,
        private val darkMode: Boolean

    ) : SimpleAdapter(context, data, resource, from, to) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view = super.getView(position, convertView, parent)

            val nadpisObsah = view.findViewById<View>(android.R.id.text1) as TextView

            val textObsah = view.findViewById<View>(android.R.id.text2) as TextView

            val nadpisPopis = data[position]["nadpis"]

            val textPopis = data[position]["text"]

            // Procházení listem názvů
            for (i in nazvyData) {

                // Pro každý nadpis stanovíme barvu podle motivu
                if (nadpisPopis == i) {

                    view.findViewById<View>(android.R.id.text1)?.apply {

                        // Barva podle motivu
                        if (darkMode) {

                            // Zbravení nadpisu
                            nadpisObsah.setTextColor(Color.parseColor("#ffc412"))
                            nadpisObsah.setTypeface(null, Typeface.BOLD)

                        } else {

                            // Zbravení nadpisu
                            nadpisObsah.setTextColor(Color.parseColor("#6200ee"))
                            nadpisObsah.setTypeface(null, Typeface.BOLD)
                        }
                    }
                }
            }

            if (textPopis == textData[3] || textPopis == textData[4]) {

                // Zbravení nadpisu
                textObsah.setTextColor(Color.parseColor("#1196ee"))
            }

            return view
        }
    }
}