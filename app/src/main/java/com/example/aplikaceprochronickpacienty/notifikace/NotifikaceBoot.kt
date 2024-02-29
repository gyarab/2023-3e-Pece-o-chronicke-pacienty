package com.example.aplikaceprochronickpacienty.notifikace

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Prehled

/** Třída pro opětné automatické zavolání notifikace pro každý následující den **/
class NotifikaceBoot : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == "android.intent.action.BOOT_COMPLETED") {

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
                    .setContentIntent(aktivita)
                    .build()

                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(notifikaceID, notifikace)
            }
        }
    }
}