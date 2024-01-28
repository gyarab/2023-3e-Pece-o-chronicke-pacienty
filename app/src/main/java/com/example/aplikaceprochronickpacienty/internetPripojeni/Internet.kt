package com.example.aplikaceprochronickpacienty.internetPripojeni

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.prihlaseni.Prihlaseni


class Internet : AppCompatActivity() {

    private lateinit var internet_button: AppCompatButton

    private lateinit var internet_imageview: ImageView

    private lateinit var intenet_loader: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supportActionBar?.hide()
        setContentView(R.layout.activity_internet)

        internet_imageview = findViewById(R.id.internet_imageview)

        internet_button = findViewById(R.id.internet_button)

        intenet_loader = findViewById(R.id.intenet_loader)

        intenet_loader.visibility = View.GONE

        val pripojeni = InternetPripojeni()

        internet_button.setOnClickListener {

            if (pripojeni.checkInternetConnection(this)) {

                intenet_loader.visibility = View.VISIBLE

                startActivity(Intent(applicationContext, Prihlaseni::class.java))

            } else {

                onShakeImage(internet_imageview)
            }
        }
    }

    /** Animace se spustí v případě, když uživatel není stále připojen k internetu **/
    private fun onShakeImage(imageView: ImageView) {

        val animaceShake = AnimationUtils.loadAnimation(applicationContext, R.anim.shake_anim)
        imageView.startAnimation(animaceShake)
    }
}