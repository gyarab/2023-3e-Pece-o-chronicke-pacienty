package com.example.aplikaceprochronickpacienty.roomDB

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uzivatel_table")
data class Uzivatel(

    @PrimaryKey(autoGenerate = false)
    val uzivatel_id: Int,
    val SubjectId: Int,
    val Date: String,
    val WeightDayKG: Double,
    val EnergyIntakeDayKJ: Double,
    val StepsCountDay: Int,

)
