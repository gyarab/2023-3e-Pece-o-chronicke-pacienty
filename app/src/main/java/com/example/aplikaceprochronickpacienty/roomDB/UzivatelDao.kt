package com.example.aplikaceprochronickpacienty.roomDB

import android.text.Editable
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

    @Query("SELECT * FROM uzivatel_table ORDER BY SubjectId = :subjectId DESC LIMIT 7")
    suspend fun getLastWeekData(subjectId : Int): List<Uzivatel>

    @Query("SELECT * FROM uzivatel_table ORDER BY SubjectId = :subjectId DESC LIMIT 1")
    suspend fun getLastDayData(subjectId : Int): List<Uzivatel>

    @Query("SELECT uzivatel_id FROM uzivatel_table ORDER BY uzivatel_id DESC LIMIT 1")
    suspend fun getLastUserColumnValue(): Int?

    @Query("UPDATE uzivatel_table SET StepsCountDay = :kroky, EnergyIntakeDayKJ = :kalorie, WeightDayKG = :vaha WHERE SubjectId = :subjectId AND Date = :datum")
    suspend fun updateUser(subjectId : Int, datum: String, kroky: Int, kalorie: Double, vaha: Double): Int

    @Query("SELECT Date FROM uzivatel_table WHERE SubjectId = :subjectId")
    suspend fun getDatesForSubject(subjectId : Int): List<String>

    @Query("SELECT StepsCountDay FROM uzivatel_table WHERE SubjectId = :subjectId ORDER BY uzivatel_id DESC LIMIT 1")
    suspend fun getSteps(subjectId : Int): Int

    @Query("UPDATE uzivatel_table SET StepsCountDay = :kroky WHERE SubjectId = :subjectId AND Date = :date")
    suspend fun updateSteps(subjectId: Int, date: String, kroky: Int): Int

    @Query("SELECT EnergyIntakeDayKJ FROM uzivatel_table WHERE SubjectId = :subjectId ORDER BY uzivatel_id DESC LIMIT 1")
    suspend fun getCalories(subjectId : Int): Double

    @Query("UPDATE uzivatel_table SET EnergyIntakeDayKJ = :kalorie WHERE SubjectId = :subjectId AND Date = :date")
    suspend fun updateCalories(subjectId: Int, date: String, kalorie: Double): Int

    @Query("SELECT WeightDayKG FROM uzivatel_table WHERE SubjectId = :subjectId ORDER BY uzivatel_id DESC LIMIT 1")
    suspend fun getWeight(subjectId : Int): Double

    @Query("UPDATE uzivatel_table SET WeightDayKG = :vaha WHERE SubjectId = :subjectId AND Date = :date")
    suspend fun updateWeight(subjectId: Int, date: String, vaha: Double): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUser(uzivatel: Uzivatel)

    @Query("DELETE FROM uzivatel_table WHERE uzivatel_id = (SELECT uzivatel_id FROM uzivatel_table ORDER BY uzivatel_id DESC LIMIT 1)")
    suspend fun deleteLastRow(): Int
}