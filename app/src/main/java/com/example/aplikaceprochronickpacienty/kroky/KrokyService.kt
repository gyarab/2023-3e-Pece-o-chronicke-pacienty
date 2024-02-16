package com.example.aplikaceprochronickpacienty.kroky

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Prehled


class KrokyService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount = 0

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        sharedPreferences = getSharedPreferences("StepCountingPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_ID, createNotification())
        startStepCounting()
        return START_STICKY
    }

    override fun onDestroy() {
        stopStepCounting()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor == stepSensor) {
                val previousStepCount = sharedPreferences.getInt(KEY_STEP_COUNT, 0)
                stepCount = it.values[0].toInt() - previousStepCount
                editor.putInt(KEY_STEP_COUNT, stepCount)
                editor.apply()
                sendStepCountBroadcast(stepCount)
            }
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        return notificationBuilder
            .setContentTitle("Step Counting Service")
            .setContentText("Counting your steps...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Counting Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startStepCounting() {
        stepSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopStepCounting() {
        sensorManager.unregisterListener(this)
    }

    private fun sendStepCountBroadcast(steps: Int) {
        //val intent = Intent(Prehled.ACTION_UPDATE_STEP_COUNT)
        //intent.putExtra(Prehled.EXTRA_STEP_COUNT, steps)
        //sendBroadcast(intent)
    }

    companion object {
        private const val FOREGROUND_ID = 101
        private const val CHANNEL_ID = "StepCountingChannel"
        private const val KEY_STEP_COUNT = "stepCount"
    }
}