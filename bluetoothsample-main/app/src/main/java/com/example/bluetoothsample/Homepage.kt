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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch

class HomePage : ComponentActivity() {
    private val bluetoothController = BluetoothController()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomePageContent(bluetoothController, this)
        }
    }
}

data class KeyboardShortcut(val name: String, val value: Int)

data class StreamDeckList(
    val name: String,
    val subElements: List<KeyboardShortcut>,
    var isSelected: Boolean = false
)
fun getDefaultStreamDeck(): List<StreamDeckList> {
    return listOf(
        StreamDeckList(
            "Function",
            listOf(
                KeyboardShortcut("F1", 58),
                KeyboardShortcut("F2", 59),
                KeyboardShortcut("F3", 60),
                KeyboardShortcut("F4", 61)
            )
        ),
        StreamDeckList(
            "Arrow",
            listOf(
                KeyboardShortcut("->", 79),
                KeyboardShortcut("<-", 80),
                KeyboardShortcut("^", 81),
                KeyboardShortcut("v", 82)
            )
        )
    )
}

@Composable
fun HomePageContent(bluetoothController: BluetoothController, activity: ComponentActivity) {
    var showBluetoothContent by remember { mutableStateOf(false) }
    var showHomePage by remember { mutableStateOf(true) }
    var showPopup by remember { mutableStateOf(false) }
    var shortcut_name by remember { mutableStateOf("") }
    var shortcut_value by remember { mutableStateOf(0) }

    var buttonsList by remember { mutableStateOf(mutableListOf<Pair<String, KeyboardShortcut>>()) }
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
                Pair(
                    buttonEntity.buttonName,
                    KeyboardShortcut(buttonEntity.shortcutName, buttonEntity.shortcutValue)
                )
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
                    buttonDao.insertButton(
                        ButtonEntity(
                            0L,
                            buttonName,
                            shortcutName.name,
                            shortcutName.value
                        )
                    )
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
            Button(
                onClick = {
                    showBluetoothContent = true
                    showHomePage = false
                },
                colors = ButtonDefaults.buttonColors(Color.Blue),
            ) {
                Text(text = "Bluetooth")
            }

            if(!buttonsList.isEmpty()) {
                Text(
                    text = "Created buttons",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(buttonsList) { (name, shortcut) ->
                    MyBLEButton(
                        text = name,
                        shortcut = shortcut.value,
                        bluetoothController = bluetoothController,
                    )
                }
            }

            Text(
                text = "Default Stream Deck",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )

            val myList by remember { mutableStateOf(getDefaultStreamDeck()) }

            for (element in myList) {
                var isSelected by remember { mutableStateOf(element.isSelected) }

                Button(
                    onClick = {
                        isSelected = !isSelected
                        element.isSelected = isSelected
                    },
                    colors = ButtonDefaults.buttonColors(Color.Blue),
                ) {
                    Text(text = element.name, color = Color.White, fontSize = 20.sp)
                }

                if (isSelected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (subElement in element.subElements) {
                            MyBLEButton(text = subElement.name, shortcut = subElement.value, bluetoothController = bluetoothController)
                        }
                    }
                }
                Spacer(modifier = Modifier.padding(6.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FloatingActionButton(
                        onClick = {                        // Supprimer le dernier bouton de la liste
                            if (buttonsList.isNotEmpty()) {
                                val lastButton = buttonsList.removeLast()
                                // Supprimer le dernier bouton de la base de données
                                activity.lifecycleScope.launch {
                                    buttonDao.deleteButton(
                                        ButtonEntity(
                                            lastButtonId,
                                            lastButton.first,
                                            lastButton.second.name,
                                            lastButton.second.value
                                        )
                                    )
                                }
                            }
                            lastButtonId -= 1
                        }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        contentColorFor(backgroundColor = Color.Red)
                    }

                    Spacer(modifier = Modifier.weight(1f)) // Ajoute un espace flexible pour séparer les boutons

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

                    Button(onClick = {
                        showBluetoothContent = false
                        showHomePage = true
                    }) {
                        Text(text = "Back to Home")
                    }
                }
            }
        }

        // Affichez la popup si showPopup est true
        if (showPopup) {
            StreamDeckDialog(
                onCreateButtonClicked = { (name, shortcut) ->
                    shortcut_name = name
                    shortcut_value = shortcut.value
                    showPopup = false
                    val defaultButton = Pair(
                        name,
                        KeyboardShortcut(shortcut_name, shortcut_value)
                    ) // Remplacez le nom et la valeur par défaut
                    buttonsList.add(defaultButton)
                    // Add the button to the local storage
                    buttonsList.forEach { (buttonName, shortcutName) ->
                        activity.lifecycleScope.launch {
                            buttonDao.insertButton(
                                ButtonEntity(
                                    0L,
                                    buttonName,
                                    shortcutName.name,
                                    shortcutName.value
                                )
                            )
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
    var selectedShortcut by remember { mutableStateOf(KeyboardShortcut("Unknown", -1)) }

    AlertDialog(
        onDismissRequest = { onCancelButtonClicked() },
        title = { Text("Create a new shortcut button") },
        text = {
            Column {
                TextField(
                    value = enteredName,
                    onValueChange = { enteredName = it },
                    label = { Text("Shortcut name") }
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
                    onClick = { expanded = true },
                    colors = ButtonDefaults.buttonColors(Color.Blue)

                ) {
                    if (clicked) {
                        Text(text = selectedShortcut.name)
                    } else {
                        Text(text = "Select a shortcut")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!enteredName.text.isNullOrEmpty() && selectedShortcut.name != "Unknown" && selectedShortcut.value != -1) {
                        onCreateButtonClicked(Pair(enteredName.text, selectedShortcut))
                    } else {
                    }
                },
                colors = ButtonDefaults.buttonColors(Color.Blue)

            ) {
                Text("Create")
            }
        },

        dismissButton = {
            Button(onClick = {
                onCancelButtonClicked()
            },
                colors = ButtonDefaults.buttonColors(Color.Red)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MyBLEButton(
    text: String,
    shortcut: Int,
    bluetoothController: BluetoothController? = null,
) {
    var cpt by remember { mutableStateOf(false) }

    Button(onClick = {
        cpt = true
    },
        colors = ButtonDefaults.buttonColors(Color.Black),
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp)
    }

    if (cpt) {
        if (bluetoothController != null) {
            BluetoothDeskContainer(bluetoothController = bluetoothController, shortcut = shortcut)
        }
        cpt = false
    }
}