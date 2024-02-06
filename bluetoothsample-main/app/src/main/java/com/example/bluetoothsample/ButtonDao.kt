package com.example.bluetoothsample

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ButtonDao {
    @Query("SELECT * FROM button_table")
    suspend fun getAllButtons(): List<ButtonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertButton(button: ButtonEntity)

    @Delete
    suspend fun deleteButton(button: ButtonEntity)

    // Delete all buttons
    @Query("DELETE FROM button_table")
    suspend fun deleteAll()

}
