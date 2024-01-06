package com.example.aplikaceprochronickpacienty.roomDB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Uzivatel::class], version = 1, exportSchema = false)
abstract class UzivatelDatabase : RoomDatabase() {

    abstract fun uzivatelDao(): UzivatelDao

    companion object {

        @Volatile
        private var INSTANCE: UzivatelDatabase? = null

        fun getDatabase(context: Context): UzivatelDatabase {

            val tempInstance = INSTANCE

            if (tempInstance != null){

                return tempInstance
            }

            synchronized(this) {

                val instance = Room.databaseBuilder(

                    context.applicationContext,
                    UzivatelDatabase::class.java,
                    "uzivatel_database"

                ).build()

                INSTANCE = instance

                return instance
            }
        }
    }
}