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

    @Query("SELECT * FROM uzivatel_table ORDER BY SubjectId = :subjectId DESC LIMIT 30")
    suspend fun getLastMonthData(subjectId : Int): List<Uzivatel>

    @Query("SELECT uzivatel_id FROM uzivatel_table ORDER BY uzivatel_id DESC LIMIT 1")
    suspend fun getLastUserColumnValue(): Int?

    @Query("UPDATE uzivatel_table SET StepsCountDay = :kroky, EnergyIntakeDayKJ = :kalorie, WeightDayKG = :vaha WHERE SubjectId = :subjectId AND Date = :datum")
    suspend fun updateUser(subjectId : Int, datum: String, kroky: Int, kalorie: Double, vaha: Double): Int

    @Query("SELECT Date FROM uzivatel_table WHERE SubjectId = :subjectId")
    suspend fun getDatesForSubject(subjectId : Int): List<String>

    @Query("SELECT StepsCountDay FROM uzivatel_table WHERE SubjectId = :subjectId AND Date = :date")
    suspend fun getSteps(subjectId : Int, date : String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUser(uzivatel: Uzivatel)

}