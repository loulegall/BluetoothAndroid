package com.example.bluetoothsample

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room

@Composable
fun App() {
    val context = LocalContext.current
    val database = Room.databaseBuilder(
        context,
        AppDatabase::class.java, "app-database"
    ).build()

    // ...
}
