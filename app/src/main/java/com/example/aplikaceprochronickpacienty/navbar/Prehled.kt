package com.example.aplikaceprochronickpacienty.navbar

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import app.futured.donut.DonutProgressView
import app.futured.donut.DonutSection
import com.db.williamchart.view.BarChartView
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.databinding.ActivityPrehledBinding
import com.example.aplikaceprochronickpacienty.roomDB.Uzivatel
import com.example.aplikaceprochronickpacienty.roomDB.UzivatelViewModel
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.InputStream


class Prehled : AppCompatActivity() {

    private lateinit var binding: ActivityPrehledBinding

    // ROOM
    private lateinit var uzivatelViewModel: UzivatelViewModel

    // Donut
    private var prehled_donut_bar: DonutProgressView? = null

    // Chart
    private var prehled_chart_bar: BarChartView? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        binding = ActivityPrehledBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = findViewById(R.id.bottom_nav)

        navView.selectedItemId = R.id.navigation_home

        navView.setOnNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.navigation_home -> {
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigation_chat -> {
                    startActivity(Intent(applicationContext, Chat::class.java))
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigation_settings -> {
                    startActivity(Intent(applicationContext, Ucet::class.java))
                    overridePendingTransition(0, 0)
                    return@setOnNavigationItemSelectedListener true
                }

                else -> return@setOnNavigationItemSelectedListener false
            }
        }

        uzivatelViewModel = ViewModelProvider(this).get(UzivatelViewModel::class.java)

        prehled_donut_bar = findViewById(R.id.prehled_donut_bar)

        prehled_chart_bar = findViewById(R.id.prehled_barChart)


        val kroky = DonutSection(
            name = "kroky",
            color = Color.parseColor("#BC13FE"),
            amount = 3f
        )

        /*val kalorie = DonutSection(
            name = "section_2",
            color = Color.parseColor("#ff30a2"),
            amount = 3f
        )

        val vaha = DonutSection(
            name = "section_2",
            color = Color.parseColor("#09dbd0"),
            amount = 1f
        )*/



        // Donut bar setup
        prehled_donut_bar?.let { donutView ->

            donutView.cap = 5f
            donutView.submitData(listOf(kroky))

            /*donutView.addAmount(
                sectionName = "drink_amount_water",
                amount = 0.5f,
                color = Color.parseColor("#03BFFA"))*/
        }

        // Chart bar setup
        binding.apply {

            prehledBarChart.animation.duration = animationDuration
            prehledBarChart.animate(barSet)
        }


    // Vkládání dat do databáze
    ulozitData()

        /*val email = intent.getStringExtra("email")
        val displayName = intent.getStringExtra("name")

        textView.text = email + "\n" + displayName*/
    }

    var i = 1

    fun ulozitData() {

        try {

            val vstup: InputStream = assets.open("data_uzivatele.csv")

            csvReader().open(vstup) {
                readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->

                    val id_uzivatel = row.values.toList().getOrNull(0)?.toInt()

                    val datum_uzivatel = row.values.toList().getOrNull(1)

                    val vaha_uzivatel = row.values.toList().getOrNull(2)?.toDouble()

                    val kalorie_uzivatel = row.values.toList().getOrNull(3)?.toDouble()

                    val kroky_uzivatel = row.values.toList().getOrNull(4)?.toInt()

                    if (id_uzivatel != null &&
                        datum_uzivatel != null &&
                        vaha_uzivatel != null &&
                        kalorie_uzivatel != null &&
                        kroky_uzivatel != null
                    ) {

                        // Vytvoreni objektu Uzivatele
                        val uzivatel =

                            Uzivatel(i++,
                                id_uzivatel,
                                datum_uzivatel,
                                vaha_uzivatel,
                                kalorie_uzivatel,
                                kroky_uzivatel
                            )

                        println(uzivatel)

                        // Pridani dat do databaze
                        uzivatelViewModel.pridatUzivateleViewModel(uzivatel)
                    }

                }
            }

        } catch (e: Exception) {

            Log.e("ERROR VSTUP", e.toString())
        }

    }

    // Parametry grafu
    companion object {

        private val barSet = listOf(
            "JAN" to 4F,
            "FEB" to 7F,
            "MAR" to 2F,
            "MAY" to 2.3F,
            "APR" to 5F,
            "JUN" to 4F
        )

        private val horizontalBarSet = listOf(
            "PORRO" to 5F,
            "FUSCE" to 6.4F,
            "EGET" to 3F
        )

        private const val animationDuration = 1000L
    }
}