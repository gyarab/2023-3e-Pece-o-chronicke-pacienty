package com.example.aplikaceprochronickpacienty.notifikace


import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Prehled

const val notifikaceID = 1
const val kanalID = "Oznameni"
const val nadpisExtra = "Nadpis"
const val zpravaExtra = "Zprava"
const val content = "Content"

/** Třída pro zaslání notifikací do mobilního zařízení uživatele **/
class Notifikace : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val prehledIntent = Intent(context, Prehled::class.java)
        prehledIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val aktivita = PendingIntent.getActivity(
            context, notifikaceID, prehledIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        if (context != null && intent != null) {

            val notifikace = NotificationCompat.Builder(context, kanalID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(intent.getStringExtra(nadpisExtra))
                .setContentText(intent.getStringExtra(zpravaExtra))
                .setAutoCancel(true)
                .setContentIntent(aktivita)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(notifikaceID, notifikace)
        }
    }
}