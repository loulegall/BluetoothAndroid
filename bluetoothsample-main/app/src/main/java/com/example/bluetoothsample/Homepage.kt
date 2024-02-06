package com.example.bluetoothsample

import android.annotation.SuppressLint
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

data class KeyboardShortcut(val name: String, val value: Int)

data class StreamDeckList(
    val name: String,
    val subElements: List<KeyboardShortcut>,
    var isSelected: Boolean = false
)

class HomePage : ComponentActivity() {
    private val bluetoothController = BluetoothController()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomePageContent(bluetoothController, this)
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun HomePageContent(bluetoothController: BluetoothController, activity: ComponentActivity) {
    var showBluetoothContent by remember { mutableStateOf(false) }
    var showHomePage by remember { mutableStateOf(true) }
    var showPopup by remember { mutableStateOf(false) }
    var shortcutName by remember { mutableStateOf("") }
    var shortcutValue by remember { mutableIntStateOf(0) }

    var buttonsList by remember { mutableStateOf(mutableListOf<Pair<String, KeyboardShortcut>>()) }
    var lastButtonId by remember { mutableLongStateOf(0L) }
    val database = Room.databaseBuilder(
        activity.applicationContext,
        AppDatabase::class.java, "app-database"
    ).build()
    val buttonDao = database.buttonDao()

    DisposableEffect(Unit) {
        // Get the buttons from the local storage when the Composable is created
        activity.lifecycleScope.launch {
            val savedButtons = buttonDao.getAllButtons()
            buttonsList = savedButtons.map { buttonEntity ->
                Pair(
                    buttonEntity.buttonName,
                    KeyboardShortcut(buttonEntity.shortcutName, buttonEntity.shortcutValue)
                )
            }.toMutableList()

            // Get the last button id
            val lastButton = savedButtons.lastOrNull()
            if (lastButton != null) {
                lastButtonId = lastButton.id
            }
        }
        onDispose {
            // Save the buttons in the local storage when the Composable is destroyed
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
            Button(
                onClick = {
                    showBluetoothContent = true
                    showHomePage = false
                },
                colors = ButtonDefaults.buttonColors(Color.Blue),
            ) {
                Text(text = "Bluetooth")
            }

            //region Created buttons
            if(buttonsList.isNotEmpty()) {
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
                    ButtonBluetooth(
                        name = name,
                        shortcut = shortcut.value,
                        bluetoothController = bluetoothController,
                    )
                }
            }
            //endregion

            //region Default Stream Deck
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
                            ButtonBluetooth(name = subElement.name, shortcut = subElement.value, bluetoothController = bluetoothController)
                        }
                    }
                }
                Spacer(modifier = Modifier.padding(6.dp))
            }
            //endregion

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FloatingActionButton(
                        onClick = {
                            // Delete the last button created
                            if (buttonsList.isNotEmpty()) {
                                val lastButton = buttonsList.removeLast()
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
                                lastButtonId -= 1
                            }
                        }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        contentColorFor(backgroundColor = Color.Red)
                    }

                    Spacer(modifier = Modifier.weight(1f))

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
            // Bluetooth connexion page
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

        if (showPopup) {
            DialogAddButton(
                onCreateButtonClicked = { (name, shortcut) ->
                    shortcutName = name
                    shortcutValue = shortcut.value
                    showPopup = false
                    val defaultButton = Pair(
                        name,
                        KeyboardShortcut(shortcutName, shortcutValue)
                    )
                    // Add the created button to the list
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

/**
 * Composable that displays a dialog to add a new shortcut button
 */
@Composable
fun DialogAddButton(
    onCreateButtonClicked: (Pair<String, KeyboardShortcut>) -> Unit,
    onCancelButtonClicked: () -> Unit
) {
    var enteredName by remember { mutableStateOf(TextFieldValue()) }
    var menuExpand by remember { mutableStateOf(false) }
    var shortcutSelected by remember { mutableStateOf(KeyboardShortcut("Unknown", -1)) }

    val keyboardShortcuts = KeyboardReport.KeyEventMap.map { (keyCode, value) ->
        val name = KeyEvent.keyCodeToString(keyCode) ?: "Unknown"
        KeyboardShortcut(name, value)
    }

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

                // Menu of keyboard shortcuts
                DropdownMenu(
                    expanded = menuExpand,
                    onDismissRequest = { menuExpand = false },
                ) {
                    keyboardShortcuts.forEach { shortcut ->
                        Box(
                            modifier = Modifier
                                .clickable {
                                    shortcutSelected = shortcut
                                    menuExpand = false
                                }
                                .padding(8.dp)
                        ) {
                            Text(text = shortcut.name)
                        }
                    }
                }

                Button(
                    onClick = { menuExpand = true },
                    colors = ButtonDefaults.buttonColors(Color.Blue)

                ) {
                    if (shortcutSelected.name != "Unknown" && shortcutSelected.value != -1) {
                        Text(text = shortcutSelected.name)
                    } else {
                        Text(text = "Select a shortcut")
                    }
                }
            }
        },
        confirmButton = {
            // Save the button if the name and the shortcut are not empty
            Button(
                onClick = {
                    if (enteredName.text.isNotEmpty() && shortcutSelected.name != "Unknown" && shortcutSelected.value != -1) {
                        onCreateButtonClicked(Pair(enteredName.text, shortcutSelected))
                    }
                },
                colors = ButtonDefaults.buttonColors(Color.Blue)

            ) {
                Text("Create")
            }
        },

        dismissButton = {
            // Cancel the button creation
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

/**
 * Returns a list of default Stream Deck buttons linked to their respective shortcuts
 */
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

/**
 * Composable that displays a button with a BluetoothDeskContainer
 */
@Composable
fun ButtonBluetooth(
    name: String,
    shortcut: Int,
    bluetoothController: BluetoothController? = null,
) {
    var bool by remember { mutableStateOf(false) }

    Button(onClick = {
        bool = true
    },
        colors = ButtonDefaults.buttonColors(Color.Black),
    ) {
        Text(text = name, color = Color.White, fontSize = 12.sp)
    }

    if (bool) {
        if (bluetoothController != null) {
            BluetoothDeskContainer(bluetoothController = bluetoothController, shortcut = shortcut)
        }
        bool = false
    }
}