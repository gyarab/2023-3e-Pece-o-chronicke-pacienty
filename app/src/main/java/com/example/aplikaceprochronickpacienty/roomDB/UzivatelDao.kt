package com.example.aplikaceprochronickpacienty.roomDB

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UzivatelDao {

    @Query("SELECT * FROM uzivatel_table ORDER BY uzivatel_id ASC")
    fun readAllData(): LiveData<List<Uzivatel>>

    @Query("SELECT * FROM uzivatel_table WHERE SubjectId = :subjectId")
    suspend fun findIdBySubjectId(subjectId : Int): List<Uzivatel>

    @Query("SELECT uzivatel_id FROM uzivatel_table ORDER BY uzivatel_id DESC LIMIT 1")
    suspend fun getLastUserColumnValue(): Int?


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun pridatUzivatele(uzivatel: Uzivatel)

}