package com.example.bluetoothsample

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.launch

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

    var buttonsList by remember { mutableStateOf(mutableListOf<Pair<String,KeyboardShortcut>>()) }
    var lastButtonId by remember { mutableStateOf(0L) }
    val database = Room.databaseBuilder(
        activity.applicationContext,
        AppDatabase::class.java, "app-database"
    ).build()
    val buttonDao = database.buttonDao()

    DisposableEffect(Unit) {
        // Récupérer les boutons de la base de données lors de la création du Composable
        activity.lifecycleScope.launch {
            val savedButtons = buttonDao.getAllButtons()
            buttonsList = savedButtons.map { buttonEntity ->
                Pair(buttonEntity.buttonName, KeyboardShortcut(buttonEntity.shortcutName, buttonEntity.shortcutValue))
            }.toMutableList()
            // get the last button id
            val lastButton = savedButtons.lastOrNull()
            if (lastButton != null) {
                lastButtonId = lastButton.id
            }
        }
        onDispose {
            // Sauvegarder les boutons dans la base de données lors de la destruction du Composable
            buttonsList.forEach { (buttonName, shortcutName) ->
                activity.lifecycleScope.launch {
                    buttonDao.insertButton(ButtonEntity(0L, buttonName, shortcutName.name, shortcutName.value))
                }
            }
        }
    }

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

            LazyColumn {
                items(buttonsList) { (name, shortcut) ->
                    MyBLEButton(
                        text = name,
                        shortcut = shortcut.value,
                        onClick = {
                        },
                        bluetoothController
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row {
                    Button(onClick = {
                        // Supprimer le dernier bouton de la liste
                        if (buttonsList.isNotEmpty()) {
                            val lastButton = buttonsList.removeLast()
                            // Supprimer le dernier bouton de la base de données
                            activity.lifecycleScope.launch {
                                buttonDao.deleteButton(ButtonEntity(lastButtonId, lastButton.first, lastButton.second.name, lastButton.second.value))
                            }
                            lastButtonId -= 1
                        }
                    }) {
                        Text(text = "DELETE LAST")
                    }

                FloatingActionButton(
                    onClick = {
                        showPopup = true
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
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
                    val defaultButton = Pair(name, KeyboardShortcut(shortcut_name,shortcut_value)) // Remplacez le nom et la valeur par défaut
                    buttonsList.add(defaultButton)
                    // Add the button to the local storage
                    buttonsList.forEach { (buttonName, shortcutName) ->
                        activity.lifecycleScope.launch {
                            buttonDao.insertButton(ButtonEntity(0L, buttonName, shortcutName.name, shortcutName.value))
                        }
                    }
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
    var selectedShortcut by remember { mutableStateOf( KeyboardShortcut("Unknown", -1)) }

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
                    println("selected value: ${selectedShortcut.value}")
                    if (!enteredName.text.isNullOrEmpty() && selectedShortcut.name != "Unknown" && selectedShortcut.value != -1 ) {
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
fun ButtonList(buttonsList: List<Pair<String, KeyboardShortcut>>, bluetoothController: BluetoothController?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for ((name, shortcut) in buttonsList) {
            MyBLEButton(
                text = name,
                shortcut = shortcut.value,
                onClick = {
                },
                bluetoothController
            )
        }
    }
}


@Composable
fun MyBLEButton(text: String,shortcut: Int, onClick: () -> Unit, bluetoothController: BluetoothController? = null) {
    var cpt by remember { mutableStateOf(false) }

    Button(onClick = {
        cpt = true;
    }) {
        Text(text = text)
    }
    if (cpt) {
        if (bluetoothController != null) {
            BluetoothDeskContainer(bluetoothController = bluetoothController,shortcut = shortcut)
        }
        cpt = false;
    }
}
