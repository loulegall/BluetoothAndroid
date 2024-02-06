package com.example.bluetoothsample

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "button_table")
data class ButtonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "button_name")
    val buttonName: String,

    @ColumnInfo(name = "shortcut_name")
    val shortcutName: String,

    @ColumnInfo(name = "shortcut_value")
    val shortcutValue: Int
)
