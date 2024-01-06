package com.example.aplikaceprochronickpacienty.roomDB

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UzivatelDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun pridatUzivateleDao(uzivatel: Uzivatel)

    @Query("SELECT * FROM uzivatel_table ORDER BY uzivatel_id ASC")
    fun readAllDataDao(): LiveData<List<Uzivatel>>

}