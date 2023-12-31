package com.example.aplikaceprochronickpacienty.navbar

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikaceprochronickpacienty.models.Message
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import com.example.aplikaceprochronickpacienty.BuildConfig
import com.example.aplikaceprochronickpacienty.R
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
import com.example.aplikaceprochronickpacienty.adapters.ChatAdapter
import com.example.aplikaceprochronickpacienty.databinding.ActivityChatBinding
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
import java.io.InputStream
import java.util.ArrayList
import java.util.UUID

class Chat : AppCompatActivity() {

    private var messageList: ArrayList<Message> = ArrayList()

    //dialogFlow
    private var sessionsClient: SessionsClient? = null
    private var sessionName: SessionName? = null
    private val uuid = UUID.randomUUID().toString()
    private val TAG = "chat"

    private lateinit var chatAdapter: ChatAdapter

    private lateinit var binding: ActivityChatBinding

    private val client = OkHttpClient()

    private var otazka: String = ""

    private var arrayList = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = findViewById(R.id.bottom_nav)

        navView.selectedItemId = R.id.navigation_chat

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

        //setting adapter to recyclerview
        chatAdapter = ChatAdapter(this, messageList)
        binding.chatView.adapter = chatAdapter

        binding.btnSend.setOnClickListener {

            otazka = binding.editMessage.text.toString()

            if (otazka.isNotEmpty()) {

                addMessageToList(otazka, false)
                sendMessageToBot(otazka)

            } else {
                Toast.makeText(this@Chat, "Please enter text!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // nacteni souboru JSON a vytvoření motivační hlášky
        readJSON()

        //initialize bot config
        setUpBot()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addMessageToList(message: String, isReceived: Boolean) {

        messageList.add(Message(message, isReceived))
        binding.editMessage.setText("")
        chatAdapter.notifyDataSetChanged()
        binding.chatView.layoutManager?.scrollToPosition(messageList.size - 1)
    }

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

    private fun sendMessageToBot(message: String) {
        val input = QueryInput.newBuilder()
            .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build()
        GlobalScope.launch {
            sendMessageInBg(input)
        }
    }

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


    private fun updateUI(response: DetectIntentResponse) {

        val botReply: String = response.queryResult.fulfillmentText

        if (botReply.isNotEmpty()) {

            addMessageToList(botReply, true)

        } else {

            addMessageToList("Typing...",true)

            Log.d("OTAZKA", otazka)

            getResponse(otazka) { result ->

                runOnUiThread {

                    messageList.remove(Message("Typing...", true))
                    addMessageToList(result, true)
                }
            }
        }
    }

    private fun readJSON(): String? {

        var json: String? = null

        try {

            val vstup: InputStream = assets.open("data-uzivatele.json")
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

            val motivacniHlaska = "Vytvoř jednovětnou motivační hlášku pro člověka, který vykonal tyto aktivity za jeden den: $kroky $spanek $aktivniPohyb"

            arrayList.add(motivacniHlaska)

            addMessageToList("Typing...",true)

            /** Motivační hláška **/

            /*getResponse(motivacniHlaska) { result ->

                runOnUiThread {

                    messageList.remove(Message("Typing...", true))
                    addMessageToList(result, true)
                }
            }*/


        } catch (e:Exception){

            Log.e("ERROR VSTUP", e.toString())
        }

        return json
    }
}
