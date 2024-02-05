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
import androidx.compose.material3.Button


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
    var shortcut_name by remember { mutableStateOf("") }
    var shortcut_value by remember { mutableStateOf(0) }

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
            if (shortcut_name.isNotEmpty()) {
                Text(text = "Nom du Shortcut : $shortcut_name")
                Text(text = "Valeur du Shortcut : $shortcut_value")
            }

            MyBLEButton(
                text = shortcut_name,
                shortcut = 8,
                onClick = { showPopup = true // To remove
                }, bluetoothController
            )

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
                onCreateButtonClicked = { (name,shortcut) ->
                    shortcut_name = name
                    shortcut_value = shortcut.value
                    showPopup = false
                },
                onCancelButtonClicked = {
                    showPopup = false
                }
            )
        }

    }
}
data class KeyboardShortcut(val name: String, val value: Int)

@Composable
fun StreamDeckDialog(
    onCreateButtonClicked: (Pair<String, KeyboardShortcut>) -> Unit,
    onCancelButtonClicked: () -> Unit
) {
    var enteredName by remember { mutableStateOf(TextFieldValue()) }
    var expanded by remember { mutableStateOf(false) }
    var clicked by remember { mutableStateOf(false) }

    val keyboardShortcuts = KeyboardReport.KeyEventMap.map { (keyCode, value) ->
        val name = KeyEvent.keyCodeToString(keyCode) ?: "Unknown"
        KeyboardShortcut(name, value)
    }
    var selectedShortcut by remember { mutableStateOf(keyboardShortcuts[0]) }

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

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    keyboardShortcuts.forEach { shortcut ->
                        Box(
                            modifier = Modifier
                                .clickable {
                                    selectedShortcut = shortcut
                                    expanded = false
                                    clicked = true
                                }
                                .padding(8.dp)
                        ) {
                            Text(text = shortcut.name)
                        }
                    }
                }

                Button(
                    onClick = { expanded = true }
                ) {
                    if (clicked) {
                        Text(text = selectedShortcut.name)
                    } else {
                        Text(text = "Select a shortcut")
                    }                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!enteredName.text.isNullOrEmpty() && selectedShortcut.name.isNotEmpty()) {
                        onCreateButtonClicked(Pair(enteredName.text, selectedShortcut))
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
@Composable
fun MyBLEButton(text: String,shortcut: Int, onClick: () -> Unit, bluetoothController: BluetoothController? = null) {
    var cpt by remember { mutableStateOf(false) }
    println("MyBLEButton")

    Button(onClick = {
        println("MyBLEButton ++")

        cpt = true;
    }) {
        Text(text = text)
    }
    if (cpt) {
        println("MyBLEButton GO")

        if (bluetoothController != null) {
            BluetoothDeskContainer(bluetoothController = bluetoothController,shortcut = shortcut)
        }
        cpt = false;
    }
}
