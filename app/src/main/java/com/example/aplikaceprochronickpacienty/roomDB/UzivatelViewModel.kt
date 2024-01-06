package com.example.aplikaceprochronickpacienty.roomDB

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UzivatelViewModel(app: Application) : AndroidViewModel(app) {

    private val readAllData: LiveData<List<Uzivatel>>
    private val repository: UzivatelRepository

    init {
        val uzivatelDao = UzivatelDatabase.getDatabase(app).uzivatelDao()
        repository = UzivatelRepository(uzivatelDao)
        readAllData = repository.readAllData
    }

    fun pridatUzivateleViewModel(uzivatel: Uzivatel) {

        viewModelScope.launch(Dispatchers.IO) {

            repository.pridatUzivateleRepository(uzivatel)
        }
    }
}