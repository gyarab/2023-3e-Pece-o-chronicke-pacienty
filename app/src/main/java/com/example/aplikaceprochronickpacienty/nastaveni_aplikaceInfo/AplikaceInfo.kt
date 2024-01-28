package com.example.aplikaceprochronickpacienty.nastaveni_aplikaceInfo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SimpleAdapter
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
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_aplikace_info)

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
            mapOf("nadpis" to " — Téma: ", "text" to "\n Mobilní aplikace pro chronicky nemocné pacienty \n"),
            mapOf("nadpis" to " — Autoři: ", "text" to "\n Vladimír Samojlov (Student) \n"),
            mapOf("nadpis" to " — Organizace: ", "text" to "\n Gymnázium Arabská \n"),
            mapOf("nadpis" to " — Zdrojový kód: ", "text" to "\n https://github.com/gyarab/2023-3e-Pece-o-chronicke-pacienty \n"),
            mapOf("nadpis" to " — Kontakt: ", "text" to "\n vladimir.samojlov@student.gyarab.cz \n")
        )

        val adapter = SimpleAdapter(
            this,
            data,
            android.R.layout.simple_list_item_2,
            arrayOf("nadpis", "text"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )

        listView.adapter = adapter

    }
}