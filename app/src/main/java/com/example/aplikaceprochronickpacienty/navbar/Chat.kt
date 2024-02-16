package com.example.aplikaceprochronickpacienty.navbar

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikaceprochronickpacienty.BuildConfig
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.adapters.ChatAdapter
import com.example.aplikaceprochronickpacienty.databinding.ActivityChatBinding
import com.example.aplikaceprochronickpacienty.internetPripojeni.Internet
import com.example.aplikaceprochronickpacienty.internetPripojeni.InternetPripojeni
import com.example.aplikaceprochronickpacienty.models.Message
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
            readJSON()

            //initialize bot config
            setUpBot()

        } else {

            startActivity(Intent(applicationContext, Internet::class.java))
        }
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

        val apiKey = BuildConfig.OPENAI_API_KEY
        val url = "https://api.openai.com/v1/chat/completions"

        val requestBody = """
        {
            "model": "gpt-3.5-turbo",
            "messages": [{"role": "system", "content": "You are a helpful assistant."},{"role": "user", "content": "$question"}],
            "max_tokens": 500,
            "temperature": 0.7
        }
    """.trimIndent()

        Log.d("OTAZKA", question)

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

    /** Odpověď Dialogflow nebo ChatGPT **/
    private fun updateUI(response: DetectIntentResponse) {

        val botReply: String = response.queryResult.fulfillmentText

        var bmi = false

        val arr = ArrayList<String>()

        getAllFormsWord("bmi",arr)

        for (i in arr) {

            if (otazka.contains(i)) {

                getBMI()
                bmi = true
            }
        }

        if (!bmi) {

            if (botReply.isNotEmpty()) {

                // Odpověď dialogFlow
                addMessageToList(botReply, true)

            } else {

                addMessageToList("...", true)

                Log.d("OTAZKA", otazka)

                getResponse(otazka) { result ->

                    runOnUiThread {

                        messageList.remove(Message("...", true))
                        addMessageToList(result, true)
                    }
                }
            }
        }
    }

    /** Přečtení souboru JSON **/
    private fun readJSON() {

        //var json: String? = null

        try {

            /*val vstup: InputStream = assets.open("data-uzivatele.json")
            json = vstup.bufferedReader().use{it.readText()}

            val arr = JSONArray(json)

            /*for (i in 0 until arr.length()){

                var objektJSON = arr.getJSONObject(i)
                arrayList.add(objektJSON.getString("steps_per_day"))
            }*/

            var objektJSON = arr.getJSONObject(2)

            val kroky = "Kroky: " + objektJSON.getString("steps_per_day") + " kroků"
            val spanek = "Spánek: " + objektJSON.getString("hours_of_sleep_per_day") + " hodin"
            val aktivniPohyb = "Aktivní pohyb: " + objektJSON.getString("active_minutes") + " minut"

            val motivacniHlaska = "Vytvoř jednovětnou motivační hlášku pro člověka, který vykonal tyto aktivity za jeden den: $kroky $spanek $aktivniPohyb"*/

            //addMessageToList("Typing...",true)

            /** Uvítání uživatele **/

            getResponse("") { vysledek ->

                runOnUiThread {

                    addMessageToList("Dobrý den, \n jak vám mohu pomoci? ", true)

                    addMessageToList(
                        "Prosím, vyberte jednu z následujících nemocí: " +
                                "\n — Obezita (Nadváha)" +
                                "\n — Kašel" +
                                "\n — Horečka" +
                                "\n — Bolest hlavy", true
                    )
                }
            }

            /** Zpracování dat uživatele **/

            runBlocking {

                // Data za poslední měsíc
                val data = roomDatabase.uzivatelDao().getLastMonthData(aktivniUzivatel)
                println("fhčuhtihjwth" + data)

                /*getResponse("Analyzuj data tohoto uživatele: $data") { result ->

                    runOnUiThread {

                        messageList.remove(Message("...", true))
                        addMessageToList(result, true)
                    }
                }*/
            }


        } catch (e: Exception) {

            Log.e("ERROR VSTUP", e.toString())
        }
    }

    /** Metoda pro výpočet BMI **/
    private fun getBMI() {

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

                    val vyskaM = vyska / 100.00

                    // BMI = tělesná váha (kg) / tělesná výška^2 (m)
                    val BMI = (vaha / (vyskaM * vyskaM))

                    val vysledek = String.format("%.1f", BMI).toDouble()

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

    private fun getAllFormsWord(slovo: String, arr: ArrayList<String>): ArrayList<String> {

        arr.add(slovo)

        for (i in 0 .. slovo.length - 1) {

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

                val pismena = slovo[i].toString() + slovo[i + 1].toString() + slovo[i + 2].toString()

                val velkaPismena = pismena.uppercase()

                val x = slovo.replace(pismena, velkaPismena)

                arr.add(x)
            }

            if (slovo.length >= 4 && i < slovo.length - 3) {

                val pismena = slovo[i].toString() + slovo[i + 1].toString() + slovo[i + 2].toString() + slovo[i + 3].toString()

                val velkaPismena = pismena.uppercase()

                val x = slovo.replace(pismena, velkaPismena)

                arr.add(x)
            }
        }

        println(arr)
        return arr
    }
}
