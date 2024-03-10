package com.example.aplikaceprochronickpacienty.upravaUdaju

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.example.aplikaceprochronickpacienty.R
import com.example.aplikaceprochronickpacienty.navbar.Prehled
import com.example.aplikaceprochronickpacienty.roomDB.UzivatelDatabase
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import fr.tvbarthel.lib.blurdialogfragment.SupportBlurDialogFragment
import kotlinx.coroutines.runBlocking


class UpravaUdaju : SupportBlurDialogFragment() {

    private lateinit var fragment_nadpis: TextView

    private lateinit var fragment_ulozit_button: Button

    private lateinit var fragment_edittext_data: TextInputEditText

    private var typData = ""

    private var dataTypNadpis = ""

    private var dataTypHint = ""

    private val aktivniUzivatel = 1648

    fun dataType(typ: String, dataNadpis: String, dataHint: String) {

        typData = typ

        dataTypNadpis = dataNadpis

        dataTypHint = dataHint
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n", "ResourceAsColor")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_uprava_udaju, container, false)

        if (dialog != null && dialog!!.window != null) {

            dialog!!.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.prehled_fragment_design))
            dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

        fragment_nadpis = root.findViewById(R.id.fragment_nadpis)

        //println("DATA: $dataTyp")

        fragment_nadpis.text = "Upravit $dataTypNadpis"

        fragment_edittext_data = root.findViewById(R.id.fragment_edittext_data)

        //println("HINT $dataTypHint")

        fragment_edittext_data.setHint("Napiš $dataTypHint")
        fragment_edittext_data.setHintTextColor(R.color.tmave_seda)

        fragment_ulozit_button = root.findViewById(R.id.fragment_ulozit_button)

        // ROOM
        val roomDatabase = context?.let { UzivatelDatabase.getDatabase(it) }

        fragment_ulozit_button.setOnClickListener {

            val datum = Prehled().dnesniDatum()

            runBlocking {

                when (typData) {

                    "kroky" -> {

                        roomDatabase?.uzivatelDao()
                            ?.updateSteps(
                                aktivniUzivatel,
                                datum,
                                fragment_edittext_data.text.toString().toInt()
                            )

                    }
                    "kalorie" -> {

                        roomDatabase?.uzivatelDao()
                            ?.updateCalories(
                                aktivniUzivatel,
                                datum,
                                fragment_edittext_data.text.toString().toDouble()
                            )
                    }

                    "vaha" -> {

                        roomDatabase?.uzivatelDao()
                            ?.updateWeight(
                                aktivniUzivatel,
                                datum,
                                fragment_edittext_data.text.toString().toDouble()
                            )
                    }

                    else -> true
                }
            }

            dismiss()
        }

        return root
    }

}