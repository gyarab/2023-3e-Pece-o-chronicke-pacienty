package com.example.aplikaceprochronickpacienty.nastaveni_aplikaceInfo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikaceprochronickpacienty.R
import com.github.barteksc.pdfviewer.PDFView

class ZobrazeniPDF : AppCompatActivity() {

    // Zobrazení PDF
    private lateinit var zobrazenipdf_viewer: PDFView
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_zobrazeni_pdf)

        // Zobrazení PDF
        zobrazenipdf_viewer = findViewById(R.id.zobrazenipdf_viewer)

        // Zobrazení dokumentace RP
        zobrazenipdf_viewer.fromAsset("DOKUMENTACE.pdf").load()
    }
}