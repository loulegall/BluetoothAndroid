package com.example.bluetoothsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.android.material.button.MaterialButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp


class HomePage : ComponentActivity() {
    private val bluetoothController = BluetoothController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomePageContent(bluetoothController)
        }
    }
}

@Composable
fun HomePageContent(bluetoothController: BluetoothController) {
    var showBluetoothContent by remember { mutableStateOf(false) }
    var showHomePage by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showHomePage) {
            // Page d'accueil
            MyButton(
                text = "Bluetooth",
                onClick = {
                    showBluetoothContent = true
                    showHomePage = false
                }
            )
            BluetoothDesk(bluetoothController)
            FloatingActionButton(
                onClick = {
                    // TODO: Add action
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.End)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        } else {
            // Page Bluetooth
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BluetoothUiConnection(bluetoothController)

                    // Bouton pour revenir à la page d'accueil
                    MyButton(
                        text = "Back to Home",
                        onClick = {
                            showBluetoothContent = false
                            showHomePage = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MyButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = text)
    }
}
