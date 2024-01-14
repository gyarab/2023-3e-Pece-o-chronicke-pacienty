package com.example.aplikaceprochronickpacienty.navbar

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import app.futured.donut.DonutProgressView
import app.futured.donut.DonutSection
import com.db.williamchart.ExperimentalFeature
import com.db.williamchart.data.AxisType
import com.db.williamchart.data.Paddings
import com.db.williamchart.data.Scale
import com.db.williamchart.data.configuration.ChartConfiguration
import com.db.williamchart.view.BarChartView
import com.db.williamchart.view.LineChartView
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.databinding.ActivityPrehledBinding
import com.example.aplikaceprochronickpacienty.roomDB.Uzivatel
import com.example.aplikaceprochronickpacienty.roomDB.UzivatelDatabase
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.play.integrity.internal.t
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream


class Prehled : AppCompatActivity() {

    private lateinit var binding: ActivityPrehledBinding

    // Donut
    private var prehled_donut_bar: DonutProgressView? = null

    // BarChart
    private var prehled_chart_bar: BarChartView? = null

    // BarChart
    private var prehled_line_bar: LineChartView? = null

    // ROOM Database
    private lateinit var roomDatabase: UzivatelDatabase

    private var vaha: ArrayList<Double> = ArrayList()
    private var datum: ArrayList<String> = ArrayList()

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

        // Databáze ROOM
        roomDatabase = UzivatelDatabase.getDatabase(this)

        // Vkládání dat do databáze
        saveData()

        prehled_donut_bar = findViewById(R.id.prehled_donut_bar)

        prehled_chart_bar = findViewById(R.id.prehled_barChart)

        prehled_line_bar = findViewById(R.id.prehled_lineChart)


        val kroky = DonutSection(
            name = "kroky",
            color = Color.parseColor("#ffc412"),
            amount = 3f
        )

        // Donut bar setup
        prehled_donut_bar?.let { donutView ->

            donutView.cap = 5f
            donutView.submitData(listOf(kroky))

            /*donutView.addAmount(
                sectionName = "drink_amount_water",
                amount = 0.5f,
                color = Color.parseColor("#03BFFA"))*/
        }

        // Informace o grafu BarChart
        val barSet = listOf(
            "PO" to 4F,
            "ÚT" to 12F,
            "ST" to 2F,
            "ČT" to 2F,
            "PÁ" to 5F,
            "SO" to 4F,
            "NE" to 1F
        )

        // Parametry grafu chartBar
        prehled_chart_bar?.let { chartBar ->

            // délka animace vykreslení grafu
            chartBar.animation.duration = 1000L

            // přidání údajů do grafu
            chartBar.animate(barSet)
        }

        /*val email = intent.getStringExtra("email")
        val displayName = intent.getStringExtra("name")

        textView.text = email + "\n" + displayName*/
    }

    var i = 1

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveData() {

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

                            Uzivatel(
                                i++,
                                id_uzivatel,
                                datum_uzivatel,
                                vaha_uzivatel,
                                kalorie_uzivatel,
                                kroky_uzivatel
                            )

                        GlobalScope.launch(Dispatchers.IO) {

                            roomDatabase.uzivatelDao().pridatUzivatele(uzivatel)
                        }
                    }
                }
            }

            // Načtení dat
            readData()

        } catch (e: Exception) {

            Log.e("ERROR VSTUP", e.toString())
        }

    }

    private suspend fun displayData(uzivatelList: List<Uzivatel>) {

        val set = mutableListOf<Pair<String, Float>>()

        withContext(Dispatchers.Main) {

            for (uzivatel in uzivatelList) {

                vaha.add(uzivatel.WeightDayKG)
                datum.add(uzivatel.Date)
            }

            for (i in 0..vaha.size step 3) {

                set.add(datum.get(i) to vaha.get(i).toFloat())
            }
        }

        // Parametry grafu lineBar
        linearChart(set)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun readData() {
        GlobalScope.launch {

            val uzivatelList: List<Uzivatel> = roomDatabase.uzivatelDao().findIdBySubjectId(2285)
            displayData(uzivatelList)
        }
    }

    @OptIn(ExperimentalFeature::class)
    @SuppressLint("SetTextI18n")
    private fun linearChart(set: MutableList<Pair<String, Float>>) {

        prehled_line_bar?.let { lineChart ->

            lineChart.gradientFillColors =
                intArrayOf(
                    Color.parseColor("#09dbd0"),
                    Color.TRANSPARENT
                )
            lineChart.animation.duration = 1000L

            // Vypsání souřadnice grafu při dotyku uživatele
            lineChart.onDataPointTouchListener = { index, _, _ ->

                val x = set[index].first
                val y = set[index].second

                val barvaX = "<font color='#ffc412'>X: </font>"
                val barvaY = "<font color='#ffc412'> Y: </font>"
                binding.prehledSouradniceLinearChart.text = Html.fromHtml(barvaX + x + barvaY + "$y")

                println("[X: $x, Y: $y]")
            }

            lineChart.animate(set)
        }
    }
}