package com.example.aplikaceprochronickpacienty.navbar

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aplikaceprochronickpacienty.BuildConfig
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.adapters.ChatAdapter
import com.example.aplikaceprochronickpacienty.databinding.ActivityChatBinding
import com.example.aplikaceprochronickpacienty.internetPripojeni.Internet
import com.example.aplikaceprochronickpacienty.internetPripojeni.InternetPripojeni
import com.example.aplikaceprochronickpacienty.models.Message
import com.example.aplikaceprochronickpacienty.notifikace.Notifikace
import com.example.aplikaceprochronickpacienty.notifikace.kanalID
import com.example.aplikaceprochronickpacienty.notifikace.nadpisExtra
import com.example.aplikaceprochronickpacienty.notifikace.notifikaceID
import com.example.aplikaceprochronickpacienty.notifikace.zpravaExtra
import com.example.aplikaceprochronickpacienty.roomDB.UzivatelDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.DetectIntentRequest
import com.google.cloud.dialogflow.v2.DetectIntentResponse
import com.google.cloud.dialogflow.v2.QueryInput
import com.google.cloud.dialogflow.v2.SessionName
import com.google.cloud.dialogflow.v2.SessionsClient
import com.google.cloud.dialogflow.v2.SessionsSettings
import com.google.cloud.dialogflow.v2.TextInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.HashMap
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.UUID
import java.util.concurrent.TimeUnit


class Chat : AppCompatActivity() {

    private var messageList: ArrayList<Message> = ArrayList()

    //dialogFlow
    private var sessionsClient: SessionsClient? = null
    private var sessionName: SessionName? = null
    private val uuid = UUID.randomUUID().toString()
    private val TAG = "chat"

    // Chat Adapter
    private lateinit var chatAdapter: ChatAdapter

    private lateinit var binding: ActivityChatBinding

    // Zavolání klienta pro prpojení s dialogFlow
    private var client = OkHttpClient()

    // Otázka uživatele
    private var otazka: String = ""

    // Vstupní text uživatele
    private lateinit var textView: TextView

    // ROOM Database
    private lateinit var roomDatabase: UzivatelDatabase

    // Aktivní uživatel
    private val aktivniUzivatel = 2285

    private var motivacniHlaska = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = findViewById(R.id.bottom_nav)

        navView.selectedItemId = R.id.navigation_chat

        val pripojeni = InternetPripojeni()

        if (pripojeni.checkInternetConnection(this)) {

            navView.setOnNavigationItemSelectedListener { item ->

                when (item.itemId) {

                    R.id.navigation_home -> {

                        startActivity(Intent(applicationContext, Prehled::class.java))
                        overridePendingTransition(0, 0)
                        return@setOnNavigationItemSelectedListener true
                    }

                    R.id.navigation_chat -> {
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

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) !=
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.USE_EXACT_ALARM) !=
                PackageManager.PERMISSION_GRANTED) {

                // Request the permission
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM, Manifest.permission.USE_EXACT_ALARM),
                    1000)
            }

            createNotification()

            // Databáze ROOM
            roomDatabase = UzivatelDatabase.getDatabase(this)

            // Nadpis AI ChatBota
            textView = findViewById(R.id.textView)

            // Barvy textu
            val barva = textView.paint
            val sirkaTextu = barva.measureText("AI ChatBot")

            val gradient: Shader = LinearGradient(
                0f, 0f, sirkaTextu, textView.textSize, intArrayOf(
                    Color.parseColor("#BC13FE"),
                    Color.parseColor("#09dbd0"),
                ), null, Shader.TileMode.CLAMP
            )
            textView.paint.setShader(gradient)

            //Nastavení adapteru pro RecycleView
            chatAdapter = ChatAdapter(this, messageList)
            binding.chatView.adapter = chatAdapter

            // Kliknutí na tlačítko odeslání zprávy
            binding.btnSend.setOnClickListener {

                otazka = binding.editMessage.text.toString()

                if (otazka.isNotEmpty()) {

                    addMessageToList(otazka, false)
                    sendMessageToBot(otazka)

                } else {
                    Toast.makeText(this@Chat, "Zpráva nemůže být prázdná!", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()


            // Načtení dat uživatele
            uvitaciText()

            motivacniHlaska("tyden")

            //initialize bot config
            setUpBot()

        } else {

            startActivity(Intent(applicationContext, Internet::class.java))
        }
    }

    /** Vygenerování motivační hlášky pro uživatele **/
    private fun motivacniHlaska(obdobi: String) {

        val databazeFirebase: FirebaseDatabase = FirebaseDatabase.getInstance()
        val referenceFirebaseUzivatel: DatabaseReference =
            databazeFirebase.getReference("users")

        val uzivatel = FirebaseAuth.getInstance().currentUser!!

        referenceFirebaseUzivatel.addListenerForSingleValueEvent(object :
            ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val vaha = uzivatel.displayName?.let {
                    snapshot.child(it).child("vaha").getValue(Any::class.java).toString().toDouble()
                }

                val vyska = uzivatel.displayName?.let {
                    snapshot.child(it).child("vyska").getValue(Any::class.java).toString().toInt()
                }

                val krokyCil = uzivatel.displayName?.let {
                    snapshot.child(it).child("krokyCil").getValue(Any::class.java).toString()
                        .toString()
                }

                val vahaCil = uzivatel.displayName?.let {
                    snapshot.child(it).child("vahaCil").getValue(Any::class.java).toString()
                }

//                val nemoc = uzivatel.displayName?.let {
//                    snapshot.child(it).child("nemoc").getValue(Any::class.java).toString()
//                }

                runBlocking {

                    val data = HashMap<String, String>()

                    // Data za poslední měsíc
                    val mesic = roomDatabase.uzivatelDao().getLastMonthData(aktivniUzivatel).toString()

                    // Data za poslední týden
                    val tyden = roomDatabase.uzivatelDao().getLastWeekData(aktivniUzivatel).toString()

                    // Data za poslední týden
                    val den = roomDatabase.uzivatelDao().getLastDayData(aktivniUzivatel).toString()

                    if (obdobi == "mesic") {

                        data.put(mesic, "mesic")

                    } else if (obdobi == "tyden") {

                        data.put(tyden, "tyden")

                    } else {

                        data.put(den, "den")
                    }

                    // Dnešní kroky
                    val kroky =
                        roomDatabase.uzivatelDao().getSteps(aktivniUzivatel, dnesniDatum())

                    // Dnešní spálené kalorie
                    val kalorie = roomDatabase.uzivatelDao()
                        .getCalories(aktivniUzivatel, dnesniDatum())

                    if (vyska != null && vaha != null) {

                        val BMI = vypocetBMI(vyska, vaha)

                        val dataUzivatele =

                            "Jseš terapeut, jehož úkol je motivovat pacienta tak aby zhubnul. " +
                            "Zde máš aktuální data (váhu, přijaté kalorie, kroky za den) od pacienta:  " +

                                    " Dnešní kroky: $kroky" +
                                    " Přijaté kalorie: $kalorie kJ" +
                                    " Váha: $vaha kg" +
                                    " Výška: $vyska cm" +
                                    " Cíl kroků za den: $krokyCil " +
                                    " Cílová váha: $vahaCil kg" +
                                    " BMI: $BMI" +
                                    " Data za poslední měsíc jsou: ${data.keys}" +

                                " Tvým úkolem je na základě poskytnutých dat (váha, kalorie, hmotnost) za poslední ${data.values}, " +
                                "motivovat pacienta tak, aby měl lepší výsledky další ${data.values}. " +
                                "V případě, když uživatel má málo kroků nebo se váha nesnižuje, buď přísný a mírně vulgární! " +
                                "Naopak pokud uživatel má spoustu kroků a vidíš, že se váha snižuje, pochval ho a motivuj dále! " +
                                "Maximální počet znaků pro tvoji odpověď je 150! Na konci odpovědi použij emoji a tagy. "

                        println(dataUzivatele)

                        motivacniHlaska = true

                        // Vygenerování motivační hlášky za určité období
                        getResponse(dataUzivatele) { result ->
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun checkNotificationPermission(): Boolean {

        return (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.USE_EXACT_ALARM) ==
                PackageManager.PERMISSION_GRANTED)
    }

    /** Přidání zprávy **/
    @SuppressLint("NotifyDataSetChanged")
    private fun addMessageToList(message: String, isReceived: Boolean) {

        messageList.add(Message(message, isReceived))
        binding.editMessage.setText("")
        chatAdapter.notifyDataSetChanged()
        binding.chatView.layoutManager?.scrollToPosition(messageList.size - 1)
    }


    /** Propojení Dialogwflow s aplikací **/
    private fun setUpBot() {

        try {
            val stream = this.resources.openRawResource(R.raw.dialogflow_credentials)
            val credentials: GoogleCredentials = GoogleCredentials.fromStream(stream)
                .createScoped("https://www.googleapis.com/auth/cloud-platform")
            val projectId: String = (credentials as ServiceAccountCredentials).projectId
            val settingsBuilder: SessionsSettings.Builder = SessionsSettings.newBuilder()
            val sessionsSettings: SessionsSettings = settingsBuilder.setCredentialsProvider(
                FixedCredentialsProvider.create(credentials)
            ).build()
            sessionsClient = SessionsClient.create(sessionsSettings)
            sessionName = SessionName.of(projectId, uuid)
            Log.d(TAG, "projectId : $projectId")

        } catch (e: Exception) {
            Log.d(TAG, "setUpBot: " + e.message)
        }
    }

    /** Poslání zprávy do Dialogflow, získání odpovědi **/
    @OptIn(DelicateCoroutinesApi::class)
    private fun sendMessageToBot(message: String) {

        val input = QueryInput.newBuilder()
            .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build()

        GlobalScope.launch {
            sendMessageInBg(input)
        }
    }

    /** Poslání zprávy uživatele do ChatBota **/
    private suspend fun sendMessageInBg(
        queryInput: QueryInput
    ) {
        withContext(Default) {
            try {
                val detectIntentRequest = DetectIntentRequest.newBuilder()
                    .setSession(sessionName.toString())
                    .setQueryInput(queryInput)
                    .build()
                val result = sessionsClient?.detectIntent(detectIntentRequest)

                if (result != null) {

                    runOnUiThread {
                        updateUI(result)
                    }
                }

            } catch (e: java.lang.Exception) {
                Log.d(TAG, "doInBackground: " + e.message)
                e.printStackTrace()
            }
        }
    }

    /** ChatGPT Response **/
    private fun getResponse(question: String, callback: (String) -> Unit) {

        // Získaný OpenAI API klíč
        val apiKey = BuildConfig.OPENAI_API_KEY

        // URL webové stránky, na kterou je poslána žádost o dokončení chatových promptů
        val url = "https://api.openai.com/v1/chat/completions"

        /** Podrobné specifikace požadavku
         *
         * Výběr modelu OpenAI
         * Zadání otázky modelu
         * Maximální počet znaků v odpověďi
         * Teplota – ovlivnění výstupu, může být více náhodný nebo naopak více konkrétní
         * Frequency_penalty – zaměření na opakování již řečených slov
         * Presence_penalty – snaha o vyjádření nezmíněných informací
         *
         * **/
        val requestBody = """
        {
            "model": "gpt-3.5-turbo",
            "messages": [{"role": "user", "content": "$question"}],
            "max_tokens": 200,
            "temperature": 0.75,
            "frequency_penalty": 1.25,
            "presence_penalty": 0.5
        }
    """.trimIndent()

        // Záslání specifikace žádosti na OpenAI server s pomocí Open API klíče
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("error", "API failed", e)
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string()

                if (body != null) {

                    Log.v("data", body)

                    try {
                        val jsonResponse = JSONObject(body)
                        val choices: JSONArray = jsonResponse.getJSONArray("choices")

                        if (choices.length() > 0) {

                            val message = choices.getJSONObject(0).getString("message")
                            val messageObject = JSONObject(message)
                            val content = messageObject.optString("content")
                            callback(content)

                            println(content)


                            if (motivacniHlaska) {

                                sendNotification(content.toString())
                            }


//                            val embeddingModel: EmbeddingModel = AllMiniLmL6V2EmbeddingModel()
//                            val embeddingStore: EmbeddingStore<TextSegment> = InMemoryEmbeddingStore()
//                            val ingestor: EmbeddingStoreIngestor = EmbeddingStoreIngestor.builder()
//                                .documentSplitter(DocumentSplitters.recursive(300, 0))
//                                .embeddingModel(embeddingModel)
//                                .embeddingStore(embeddingStore)
//                                .build()
//
//                            val document: Document = loadDocument(toPath("story-about-happy-carrot.txt"), TextDocumentParser())
//                            ingestor.ingest(document)
//
//                            val chain: ConversationalRetrievalChain = ConversationalRetrievalChain.builder()
//                                .chatLanguageModel(OpenAiChatModel.withApiKey(apiKey))
//                                .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
//                                .build()
//                            val answer: String = chain.execute("Otázka")
//                            println(answer)


                        } else {

                            Log.e("error", "No choices found in the response.")
                        }

                    } catch (e: JSONException) {

                        Log.e("error", "Error parsing JSON response: ${e.message}")
                    }

                } else {

                    Log.v("data", "empty")
                }

            }
        })
    }

//    fun toPath(fileName: String): Path {
//
//        return try {
//
//            val fileUrl: URL? = Chat::class.java.getResource(fileName)
//            Paths.get(fileUrl?.toURI())
//
//        } catch (e: Exception) {
//
//            throw Exception(e)
//        }
//    }

    /** Odpověď Dialogflow nebo ChatGPT **/
    private fun updateUI(response: DetectIntentResponse) {

        val botReply: String = response.queryResult.fulfillmentText

        var bmi = false

        val arr = ArrayList<String>()

        motivacniHlaska = false

        // Všechny tvary slova BMI
        getAllFormsWord("bmi", arr)

        for (i in arr) {

            if (otazka.contains(i)) {

                getBMIUzivatel()
                bmi = true
            }
        }

        if (!bmi) {

            if (botReply.isNotEmpty()) {

                // Odpověď dialogFlow
                addMessageToList(botReply, true)

            } else {

                addMessageToList("...", true)

                val databazeFirebase: FirebaseDatabase = FirebaseDatabase.getInstance()
                val referenceFirebaseUzivatel: DatabaseReference =
                    databazeFirebase.getReference("users")

                val uzivatel = FirebaseAuth.getInstance().currentUser!!

                referenceFirebaseUzivatel.addListenerForSingleValueEvent(object :
                    ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        val nemoc = uzivatel.displayName?.let {
                            snapshot.child(it).child("nemoc").getValue(Any::class.java).toString()
                        }

                        val vaha = uzivatel.displayName?.let {
                            snapshot.child(it).child("vaha").getValue(Any::class.java).toString().toDouble()
                        }

                        val vyska = uzivatel.displayName?.let {
                            snapshot.child(it).child("vyska").getValue(Any::class.java).toString().toInt()
                        }

                        val krokyCil = uzivatel.displayName?.let {
                            snapshot.child(it).child("krokyCil").getValue(Any::class.java).toString()
                                .toString()
                        }

                        val vahaCil = uzivatel.displayName?.let {
                            snapshot.child(it).child("vahaCil").getValue(Any::class.java).toString()
                        }

                        runBlocking {

                            // Data za poslední měsíc
                            val data = roomDatabase.uzivatelDao().getLastMonthData(aktivniUzivatel)

                            // Dnešní kroky
                            val kroky =
                                roomDatabase.uzivatelDao().getSteps(aktivniUzivatel, dnesniDatum())

                            // Dnešní spálené kalorie
                            val kalorie = roomDatabase.uzivatelDao()
                                .getCalories(aktivniUzivatel, dnesniDatum())

                            if (vyska != null && vaha != null) {

                                val BMI = vypocetBMI(vyska, vaha)

                                val dataUzivatele =

                                    "Zde jsou dnešní aktuální data uživatele: " +

                                            " Chronické onemocnění: $nemoc"
                                            " Dnešní kroky: $kroky" +
                                            " Spálené kalorie: $kalorie kJ" +
                                            " Váha: $vaha kg" +
                                            " Výška: $vyska cm" +
                                            " Cíl kroků za den: $krokyCil " +
                                            " Cílová váha: $vahaCil kg" +
                                            " BMI: $BMI" +
                                            " Data za poslední měsíc jsou: $data" +

                                    " Pokud uživatel má podle daného BMI nadváhu či obezitu, nesmí jíst jídla s velkou kalorickou hodnotou." +
                                    " Na otázku odpovídej s použitím těchto dat. Odpověď musí být stručná a musí odpovídat na otázku uživatele."

                                println(dataUzivatele)

                                getResponseAI(dataUzivatele)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
        }
    }

    /** Vytvoření notifikace pro uživatele **/
    private fun createNotification() {

        val nazev = "Titul"
        val popis = "Popis"
        val dulezitost = NotificationManager.IMPORTANCE_DEFAULT
        val kanal = NotificationChannel(kanalID, nazev, dulezitost)

        // Posílání zpráv na uzamčenou obrazovku
        kanal.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        kanal.description = popis

        val notifikaceManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifikaceManager.createNotificationChannel(kanal)
    }

    /** Poslání notifikace pro uživatele **/
    @SuppressLint("ScheduleExactAlarm")
    private fun sendNotification(zprava: String) {

        val intent = Intent(applicationContext, Notifikace::class.java)
        val nadpis = "Motivační hláška"

        intent.putExtra(nadpisExtra, nadpis)
        intent.putExtra(zpravaExtra, zprava)

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notifikaceID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Okamžitá notifikace
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            pendingIntent
        )

        //setTimeToPushNotifications(alarmManager, pendingIntent, 7)
    }

    /** Notifikace se zobrazí ve stejný čas v průběhu dne **/
    private fun setTimeToPushNotifications(
        alarmManager: AlarmManager,
        intent: PendingIntent,
        denniNotifikace: Int
    ) {

        val kalendar = GregorianCalendar.getInstance().apply {

            if (get(Calendar.HOUR_OF_DAY) >= denniNotifikace) {
                add(Calendar.DAY_OF_MONTH, 1)
            }

            set(Calendar.HOUR_OF_DAY, denniNotifikace)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            kalendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            intent
        )
    }

    /** Dnešní datum **/
    fun dnesniDatum(): String {

        // Dnešní datum
        val dnesniDatum = LocalDate.now()

        val format = DateTimeFormatter.ofPattern("M/dd/yyyy")

        val datum = dnesniDatum.format(format)

        return datum
    }

    /** Odpověď na otázku od AI **/
    private fun getResponseAI(veta: String) {

        getResponse(veta + otazka) { result ->

            runOnUiThread {

                messageList.remove(Message("...", true))
                addMessageToList(result, true)
            }
        }
    }

    /** Uvítací text **/
    private fun uvitaciText() {

        try {

            /** Uvítání uživatele **/

            getResponse("") { vysledek ->

                runOnUiThread {

                    addMessageToList("Dobrý den, \n jak vám mohu pomoci? ", true)

                    addMessageToList(
                        "Jsem vytrénovaný \uD83D\uDE0A \n " +
                                "\n Mohu vám poskytnout odbornou pomoc s následujícími nemocemi: \n" +
                                "\n — Obezita" +
                                "\n — Kašel" +
                                "\n — Horečka" +
                                "\n — Bolest hlavy", true
                    )
                }
            }


        } catch (e: Exception) {

            Log.e("ERROR VSTUP", e.toString())
        }
    }

    /** Metoda pro výpočet BMI **/
    private fun vypocetBMI(vyskaCM: Int, vahaKG: Double): Double {

        val vyskaM = vyskaCM / 100.00

        // BMI = tělesná váha (kg) / tělesná výška^2 (m)
        val BMI = (vahaKG / (vyskaM * vyskaM))

        val vysledek = String.format("%.1f", BMI).toDouble()

        return vysledek
    }

    /** Metoda pro zjištění akutálního BMI pacienta **/
    private fun getBMIUzivatel() {

        val databazeFirebase: FirebaseDatabase = FirebaseDatabase.getInstance()
        val referenceFirebaseUzivatel: DatabaseReference = databazeFirebase.getReference("users")

        val uzivatel = FirebaseAuth.getInstance().currentUser!!

        referenceFirebaseUzivatel.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val vaha = uzivatel.displayName?.let {
                    snapshot.child(it).child("vaha").getValue(Any::class.java).toString().toDouble()
                }

                val vyska = uzivatel.displayName?.let {
                    snapshot.child(it).child("vyska").getValue(Any::class.java).toString().toInt()
                }

                if (vaha != null && vyska != null) {

                    val vysledek = vypocetBMI(vyska,vaha)

                    if (vysledek < 18.5) {

                        addMessageToList(
                            "\n" +
                                    "Vaše BMI ($vysledek) je nižší než 18,5, spadáte tedy do kategorie podváha, což znamená, že vaše hmotnost je příliš nízká ve srovnání s vaší výškou.\n" +
                                    "\n" +
                                    "Podváha představuje zvýšené riziko zdravotních komplikací kvůli nedostatečnému příjmu živin a energie, což může negativně ovlivnit fungování těla",
                            true
                        )

                    } else if (vysledek >= 18.5 && vysledek < 25) {

                        addMessageToList(
                            "Vaše BMI ($vysledek) pohybuje v intervalu od 18,5 do 25, což odpovídá tabulkově ideální tělesné hmotnosti a naznačuje zdravější stav těla a nižší riziko zdravotních komplikací spojených s hmotností",
                            true
                        )

                    } else if (vysledek >= 25 && vysledek < 30) {

                        addMessageToList(
                            "Vaše BMI ($vysledek) pohybuje v rozmezí od 25 do 30, spadáte do kategorie nadváhy, což naznačuje, že vaše hmotnost je vyšší než je obvyklé pro vaši výšku, a zvýšené riziko výskytu různých zdravotních problémů, včetně srdečních onemocnění a diabetu typu 2",
                            true
                        )

                    } else if (vysledek >= 30 && vysledek < 35) {

                        addMessageToList(
                            "Vaše BMI ($vysledek) je vyšší než 30, jste klasifikován jako obézní prvního stupně. Nejste sám, protože v České republice má v současnosti obezitu 18 % žen a 20 % mužů",
                            true
                        )

                    } else if (vysledek >= 35 && vysledek < 40) {

                        addMessageToList(
                            "Vaše BMI vyšší než 35, jste klasifikován jako obézní druhého stupně, což často vyžaduje závažnější léčebné a životní úpravy, aby se zabránilo možným zdravotním komplikacím",
                            true
                        )

                    } else {

                        addMessageToList(
                            "Vaše BMI vyšší než 40, jste zařazen jako obézní třetího stupně. Je důležité okamžitě vyhledat lékařskou pomoc. Tento stupeň obezity je extrémně vážný a spojen s vysokým rizikem zdravotních komplikací, včetně srdečních onemocnění, diabetu, problémů s klouby a dalších",
                            true
                        )
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    /** Všechny verze slova **/
    private fun getAllFormsWord(slovo: String, arr: ArrayList<String>): ArrayList<String> {

        arr.add(slovo)

        for (i in 0..slovo.length - 1) {

            val pismeno: Char = slovo[i]

            val velkePismeno = pismeno.uppercase()

            val vysledek = slovo.replace(pismeno.toString(), velkePismeno)

            println(vysledek)

            arr.add(vysledek)

            if (slovo.length >= 2 && i < slovo.length - 1) {

                val pismena = slovo[i].toString() + slovo[i + 1].toString()

                val velkaPismena = pismena.uppercase()

                val x = slovo.replace(pismena, velkaPismena)

                arr.add(x)
            }

            if (slovo.length >= 3 && i < slovo.length - 2) {

                val pismena =
                    slovo[i].toString() + slovo[i + 1].toString() + slovo[i + 2].toString()

                val velkaPismena = pismena.uppercase()

                val x = slovo.replace(pismena, velkaPismena)

                arr.add(x)
            }

            if (slovo.length >= 4 && i < slovo.length - 3) {

                val pismena =
                    slovo[i].toString() + slovo[i + 1].toString() + slovo[i + 2].toString() + slovo[i + 3].toString()

                val velkaPismena = pismena.uppercase()

                val x = slovo.replace(pismena, velkaPismena)

                arr.add(x)
            }
        }

        println(arr)
        return arr
    }
}
