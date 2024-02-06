package com.example.bluetoothsample

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ButtonEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun buttonDao(): ButtonDao
}
