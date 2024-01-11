package com.example.aplikaceprochronickpacienty.roomDB

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uzivatel_table")
data class Uzivatel(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo val uzivatel_id: Int,
    @ColumnInfo val SubjectId: Int,
    @ColumnInfo val Date: String,
    @ColumnInfo val WeightDayKG: Double,
    @ColumnInfo val EnergyIntakeDayKJ: Double,
    @ColumnInfo val StepsCountDay: Int,

)
