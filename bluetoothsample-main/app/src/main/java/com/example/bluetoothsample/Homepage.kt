package com.example.bluetoothsample

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button

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
            StreamDeckDialog(
                onCreateButtonClicked = { newName ->
                    streamDeckName = newName
                    showPopup = false
                },
                onCancelButtonClicked = {
                    showPopup = false
                }
            )
        }

    }
}

@Composable
fun StreamDeckDialog(
    onCreateButtonClicked: (String) -> Unit,
    onCancelButtonClicked: () -> Unit
) {
    var enteredName by remember { mutableStateOf(TextFieldValue()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedShortcut by remember { mutableStateOf("Select a Shortcut") }

    AlertDialog(
        onDismissRequest = { onCancelButtonClicked() },
        title = { Text("Créer un nouveau Stream Deck") },
        text = {
            Column {
                TextField(
                    value = enteredName,
                    onValueChange = { enteredName = it },
                    label = { Text("Nom du Stream Deck") }
                )
                val items = KeyboardReport.KeyEventMap.values.map { keyCode ->
                    KeyEvent.keyCodeToString(keyCode) ?: "Unknown"
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    items.forEach { shortcut ->
                        Box(
                            modifier = Modifier
                                .clickable {
                                    selectedShortcut = shortcut
                                    expanded = false
                                }
                                .padding(8.dp)
                        ) {
                            Text(text = shortcut)
                        }
                    }
                }

                Button(
                    onClick = { expanded = true }
                ) {
                    Text(text = selectedShortcut)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!enteredName.text.isNullOrEmpty() && selectedShortcut != "Select a Shortcut") {
                        onCreateButtonClicked("${enteredName.text} - $selectedShortcut")
                    } else {
                    }
                }
            ) {
                Text("Créer")
            }
        },

        dismissButton = {
            Button(onClick = {
                onCancelButtonClicked()
            }) {
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