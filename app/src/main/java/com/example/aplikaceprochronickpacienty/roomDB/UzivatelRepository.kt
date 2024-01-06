package com.example.aplikaceprochronickpacienty.roomDB

import androidx.lifecycle.LiveData

class UzivatelRepository(private val uzivatelDao: UzivatelDao) {

    val readAllData: LiveData<List<Uzivatel>> = uzivatelDao.readAllDataDao()

    suspend fun pridatUzivateleRepository(uzivatel: Uzivatel) {

        uzivatelDao.pridatUzivateleDao(uzivatel)
    }
}