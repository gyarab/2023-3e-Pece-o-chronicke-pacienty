package com.example.aplikaceprochronickpacienty.navbar

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt


class Prehled : AppCompatActivity() {


    // Donut
    private var prehled_donut_bar: DonutProgressView? = null

    // BarChart
    private var prehled_chart_bar: BarChartView? = null

    // BarChart
    private var prehled_line_bar: LineChartView? = null

    // ROOM Database
    private lateinit var roomDatabase: UzivatelDatabase

    private var weight: ArrayList<Double> = ArrayList()
    private var date: ArrayList<String> = ArrayList()
    private var data: ArrayList<Any> = ArrayList()

    // TextView - zbývající kroky uživatele pro dosažení cíle
    private lateinit var prehled_zbyvajici_kroky: TextView

    // TextView - Počet kroků uživatele
    private lateinit var prehled_pocet_kroku: TextView

    // Souřadnice dotyku LineChart
    private lateinit var prehled_souradnice_linearChart: TextView

    // TabLayout
    private lateinit var prehled_tabLayoutBar: TabLayout
    private lateinit var prehled_tabLayoutLine: TabLayout

    // Aktuální položka v TabLayout
    private var tabItemBar: String = "KALORIE"
    private var tabItemLine: String = "TYDEN"

    val listDnu = ArrayList<String>()


    // BarChart
    private var kalorieClick: Int = 0
    private var krokyClick: Int = 0

    // LineChart
    private var tydenClick: Int = 0
    private var mesicClick: Int = 0
    private var rokClick: Int = 0

    private var click: String = "BAR LINE"

    val formatter: (Float) -> String = { value -> value.toString() }

    private lateinit var chartConfiguration: ChartConfiguration

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Nastavení výchozího grafu
        chartConfiguration = ChartConfiguration(
            0,
            0,
            Paddings(0F, 0F, 0F, 0F),
            AxisType.NONE,
            0F,
            Scale(0F, 0F),
            formatter
        )

        if (::chartConfiguration.isInitialized) {


            supportActionBar?.hide()
            val binding: ActivityPrehledBinding = ActivityPrehledBinding.inflate(layoutInflater)
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

            prehled_pocet_kroku = findViewById(R.id.prehled_pocet_kroku)

            prehled_zbyvajici_kroky = findViewById(R.id.prehled_zbyvajici_kroky)

            prehled_donut_bar = findViewById(R.id.prehled_donut_bar)

            prehled_chart_bar = findViewById(R.id.prehled_barChart)

            prehled_line_bar = findViewById(R.id.prehled_lineChart)

            prehled_tabLayoutBar = findViewById(R.id.prehled_tabLayout1)

            prehled_tabLayoutLine = findViewById(R.id.prehled_tabLayout2)

            prehled_souradnice_linearChart = findViewById(R.id.prehled_souradnice_linearChart)


            // Získání konkrétní položky z tabulky
            getItemFromTableBarChart()
            getItemFromTableLineChart()

        } else {

            println("ERROR")
        }
    }

    /** Přenos dat uživatele z csv souboru do databáze **/
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

    /** Získání podle datumu aktuálního dnu v týdnu **/
    private fun currentDayInWeek(datum: List<String>) {

        for (i in (datum.size - 7)..<datum.size) {

            val aktualniDatum = datum[i]

            println("DATUM: " + aktualniDatum)

            val datumFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

            val getDatum = datumFormat.parse(aktualniDatum)

            var denVtydnu =
                getDatum?.let { SimpleDateFormat("EEEE", Locale.getDefault()).format(it) }

            println(denVtydnu)

            when (denVtydnu) {

                "Monday" -> {

                    denVtydnu = "PO"

                }

                "Tuesday" -> {

                    denVtydnu = "ÚT"

                }

                "Wednesday" -> {

                    denVtydnu = "ST"

                }

                "Thursday" -> {

                    denVtydnu = "ČT"

                }

                "Friday" -> {

                    denVtydnu = "PÁ"

                }

                "Saturday" -> {

                    denVtydnu = "SO"

                }

                else -> {

                    denVtydnu = "NE"
                }
            }

            listDnu.add(denVtydnu)
        }

        println(listDnu)
    }

    /** Nastavení posluchače události při kliknutí na TabLayout **/
    private fun tabClickListener(tabLayout: TabLayout, onClickAction: (Int) -> Unit) {
        for (i in 0 until tabLayout.tabCount) {
            val tab: TabLayout.Tab? = tabLayout.getTabAt(i)

            tab?.view?.setOnClickListener {
                onClickAction(i)
            }
        }
    }

    private fun getItemFromTableBarChart() {

        tabClickListener(prehled_tabLayoutBar) { index ->

            when (index) {

                0 -> {

                    tabItemBar = "KALORIE"
                    kalorieClick++
                }

                1 -> {

                    tabItemBar = "KROKY"
                    krokyClick++
                }
            }

            click = "BAR"
            readData()
        }
    }

    /** Nastavení aktuálního elementu při kliknutí **/
    private fun getItemFromTableLineChart() {

        tabClickListener(prehled_tabLayoutLine) { index ->

            when (index) {

                0 -> {

                    tabItemLine = "TYDEN"
                    tydenClick++
                }

                1 -> {

                    tabItemLine = "MESIC"
                    mesicClick++
                }

                2 -> {

                    tabItemLine = "ROK"
                    rokClick++
                }

            }
            click = "LINE"
            readData()
        }
    }

    var i = 1

    /** Zobrazení grafu BarChart s kaloriemi a kroky pacienty za týden **/
    private fun displayBarChart(
        set: MutableList<Pair<String, Float>>,
        data: MutableList<Any>,
        uzivatelList: List<Uzivatel>
    ) {

        when (tabItemBar) {

            "KALORIE" -> {

                addUserInfoBarChart(set, data, uzivatelList)
            }

            "KROKY" -> {

                addUserInfoBarChart(set, data, uzivatelList)
            }
        }
    }


    /** Vložení informací do grafu BarChart **/
    private fun addUserInfoBarChart(
        set: MutableList<Pair<String, Float>>,
        data: MutableList<Any>,
        uzivatelList: List<Uzivatel>,
    ) {

        val datumy = ArrayList<String>()

        for (uzivatel in uzivatelList) {

            if (tabItemBar == "KALORIE") {

                val zaokrouhleniKalorii =
                    String.format("%.1f", uzivatel.EnergyIntakeDayKJ).toDouble()

                data.add(zaokrouhleniKalorii)


            } else {

                data.add(uzivatel.StepsCountDay)
            }
        }

        for (uzivatel in uzivatelList) {

            // přidání všech dat uživatele z databáze
            datumy.add(uzivatel.Date)
        }

        println("DATUMY: " + datumy)

        currentDayInWeek(datumy)

        var x = -1

        for (i in data.size - 7 until data.size) {

            x++

            if (x < 7) {

                set.add((listDnu[x] to data[i]) as Pair<String, Float>)

            }
        }
    }

    /** Zobrazení grafu LineChart s údaji pacienta podle týdne, měsíce, roku **/
    private fun displayLineChart(
        set: MutableList<Pair<String, Float>>,
        vaha: ArrayList<Double>,
        datum: ArrayList<String>,
        uzivatelList: List<Uzivatel>
    ) {

        when (tabItemLine) {

            "TYDEN" -> {

                addUserInfoLineChart(uzivatelList, vaha, datum, set, tydenClick, 7)
            }

            "MESIC" -> {

                addUserInfoLineChart(uzivatelList, vaha, datum, set, mesicClick, 30)
            }

            "ROK" -> {

                addUserInfoLineChart(uzivatelList, vaha, datum, set, rokClick, 365)
            }
        }
    }

    /** Vložení informací o grafu LineChart **/
    private fun addUserInfoLineChart(
        uzivatelList: List<Uzivatel>,
        vaha: ArrayList<Double>,
        datum: ArrayList<String>,
        set: MutableList<Pair<String, Float>>,
        number: Int,
        size: Int,
    ) {

        if (number == 0) {

            for (uzivatel in uzivatelList) {

                vaha.add(uzivatel.WeightDayKG)
                datum.add(uzivatel.Date)
            }

            pridatDatumVahu(datum, size, set, vaha)

        } else {

            pridatDatumVahu(datum, size, set, vaha)
        }
    }

    /** Přidání do listu grafu váhu a datum **/
    private fun pridatDatumVahu(
        datum: ArrayList<String>,
        size: Int,
        set: MutableList<Pair<String, Float>>,
        vaha: ArrayList<Double>
    ) {
        for (i in (datum.size - 1) downTo maxOf((datum.size - 1) - size, 0) step 2) {

            set.add(datum[i] to vaha[i].toFloat())
        }
    }

    /** Zobrazení dat uživatele do grafu LineChart **/
    private suspend fun displayData(uzivatelList: List<Uzivatel>) {

        val lineSet = mutableListOf<Pair<String, Float>>()

        val barSet = mutableListOf<Pair<String, Float>>()

        withContext(Dispatchers.Main) {

            displayBarChart(barSet, data, uzivatelList)
            displayLineChart(lineSet, weight, date, uzivatelList)
        }

        runOnUiThread {

            // Parametry grafu DonutProgressView
            donutProgressView(uzivatelList)
        }

        // Při kliknutí se zobrazí animace vybraného grafu
        when (click) {

            // Při prvním kliknutí se zobrazí animace obou grafů
            "BAR LINE" -> {

                barChart(barSet)
                linearChart(lineSet)

            }

            // Parametry grafu BarChart
            "BAR" -> {

                barChart(barSet)
            }

            // Parametry grafu LineChart
            "LINE" -> {
                linearChart(lineSet)
            }
        }
    }

    /** Načtení dat uživatele z databáze **/
    @OptIn(DelicateCoroutinesApi::class)
    private fun readData() {

        GlobalScope.launch {

            val uzivatelList: List<Uzivatel> = roomDatabase.uzivatelDao().findIdBySubjectId(2285)
            displayData(uzivatelList)
        }
    }

    /** Nastavení grafu DonutProgressView **/
    @SuppressLint("SetTextI18n", "ResourceAsColor")
    private fun donutProgressView(uzivatelList: List<Uzivatel>) {

        // Donut bar setup
        prehled_donut_bar?.let { donutView ->

            val kroky = ArrayList<Float>()

            for (uzivatel in uzivatelList) {

                kroky.add(uzivatel.StepsCountDay.toFloat())
            }

            val dnesniKroky = kroky.last().roundToInt()

            val donut = DonutSection(
                name = "kroky",
                color = Color.parseColor("#ffc412"),
                amount = dnesniKroky.toFloat()
            )

            donutView.animationDurationMs = 3000
            donutView.cap = 12000f
            donutView.submitData(listOf(donut))

            if (dnesniKroky.toString().length == 4) {

                val pridaniCarky =
                    dnesniKroky.toString().substring(0, 1) + "," + dnesniKroky.toString()
                        .substring(1)

                prehled_pocet_kroku.text = pridaniCarky

            } else if (dnesniKroky.toString().length == 5) {

                val pridaniCarky =
                    dnesniKroky.toString().substring(0, 2) + "," + dnesniKroky.toString()
                        .substring(2)

                prehled_pocet_kroku.text = pridaniCarky
                prehled_pocet_kroku.textSize = 45F
            }

            if (dnesniKroky >= donutView.cap) {

                prehled_zbyvajici_kroky.text = "SPLNĚNO! \uD83C\uDF89"

                // první zobrazení grafu
                if (click == "BAR LINE") {

                    // Animace textu
                    prehled_zbyvajici_kroky.animate().scaleX(0F).scaleY(0F).setDuration(500)

                        .withEndAction(Runnable {
                            prehled_zbyvajici_kroky.animate().scaleX(1F).scaleY(1F)
                                .setDuration(1500)
                        })
                }

            } else {

                prehled_zbyvajici_kroky.text =
                    "Zbývá " + (donutView.cap - dnesniKroky).roundToInt().toString() + " kroků"
            }

            // první zobrazení grafu
            if (click == "BAR LINE") {

                // Fade in - efekt pro zobrazení kroků
                val fadeIn = AlphaAnimation(0.0f, 1.0f)
                prehled_pocet_kroku.startAnimation(fadeIn)
                fadeIn.setDuration(4000)
                fadeIn.fillAfter = true

                val myShader: Shader = LinearGradient(
                    0F, 0F, 0F, 150F,
                    Color.parseColor("#ff30a2"),
                    Color.parseColor("#FFDC72"),
                    Shader.TileMode.CLAMP
                )

                prehled_pocet_kroku.paint.shader = myShader
            }

            /*donutView.addAmount(
                sectionName = "drink_amount_water",
                amount = 0.5f,
                color = Color.parseColor("#03BFFA"))*/
        }
    }

    /** Nastavení grafu BarChart **/
    @OptIn(ExperimentalFeature::class)
    private fun barChart(set: MutableList<Pair<String, Float>>) {

        // Parametry grafu chartBar
        prehled_chart_bar?.let { chartBar ->

            chartBar.barsGradientColors =
                intArrayOf(
                    Color.WHITE,
                    Color.parseColor("#FF30A2")
                )

            // délka animace vykreslení grafu
            chartBar.animation.duration = 1000L

            chartBar.animate(set)
        }
    }

    /** Nastavení grafu LineChart **/
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
                prehled_souradnice_linearChart.text =
                    Html.fromHtml(barvaX + x + barvaY + "$y")

                println("[X: $x, Y: $y]")
            }

            lineChart.animate(set)
        }
    }
}