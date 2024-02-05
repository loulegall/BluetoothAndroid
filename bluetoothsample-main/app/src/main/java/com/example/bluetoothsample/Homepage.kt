package com.example.bluetoothsample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import com.example.bluetoothsample.BluetoothController
import com.example.bluetoothsample.BluetoothDesk
import com.example.bluetoothsample.BluetoothUiConnection


class HomePage : ComponentActivity() { private val bluetoothController = BluetoothController()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomePageContent(bluetoothController, this)
        }
    }
}

@Composable
fun HomePageContent(bluetoothController: BluetoothController, activity: ComponentActivity) {
    var showBluetoothContent by remember { mutableStateOf(false) }
    var showHomePage by remember { mutableStateOf(true) }
    var showPopup by remember { mutableStateOf(false) }
    var streamDeckName by remember { mutableStateOf("") }

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
            if (streamDeckName.isNotEmpty()) {
                Text(text = "Nom du Stream Deck : $streamDeckName")
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = {
                        showPopup = true
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
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

        // Affichez la popup si showPopup est true
        if (showPopup) {
            StreamDeckDialog { newName ->
                streamDeckName = newName
                showPopup = false
            }
        }
    }
}

@Composable
fun StreamDeckDialog(onCreateButtonClicked: (String) -> Unit) {
    var enteredName by remember { mutableStateOf(TextFieldValue()) }

    AlertDialog(
        onDismissRequest = { /* Aucune action lors de la fermeture de la boîte de dialogue */ },
        title = { Text("Créer un nouveau Stream Deck") },
        text = {
            Column {
                TextField(
                    value = enteredName,
                    onValueChange = { enteredName = it },
                    label = { Text("Nom du Stream Deck") }
                )
                Button(
                    onClick = { /*TODO*/ }) {
                    Text(text = "Shortcut")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!enteredName.text.isNullOrEmpty()) {
                        onCreateButtonClicked(enteredName.text)
                    } else {
                        // Affichez un message d'erreur si le nom est vide
                        // Ici, vous pouvez remplacer par un Toast ou Snackbar
                    }
                }
            ) {
                Text("Créer")
            }
        },
        dismissButton = {
            Button(onClick = { /* Aucune action lors de l'annulation */ }) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun MyButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = text)
    }
}