package com.example.aplikaceprochronickpacienty.nastaveni

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

class InternetPripojeni {

    /** Kontrola, zda je zařízení připojeno přes internet **/
    fun checkInternetConnection(context: Context): Boolean {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        if (capabilities != null) {

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {

                Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                return true

            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {

                Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                return true

            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {

                Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                return true
            }
        }

        return false
    }
}