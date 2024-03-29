package com.example.bluetoothsample

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

data class IconInfo(val name: String, val icon: ImageVector)

class HomePage : ComponentActivity() {
    private val bluetoothController = BluetoothController()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureBluetoothPermission(this)
        setContent {
            HomePageContent(bluetoothController, this)
        }
    }
}
private fun ensureBluetoothPermission(activity: ComponentActivity) {
    val requestPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()){
            isGranted: Boolean ->
        if (isGranted) {Log.d(MainActivity.TAG, "Bluetooth connection granted")
        } else { Log.e(MainActivity.TAG, "Bluetooth connection not granted, Bye!")
            activity.finish()
        }
    }

    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
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
    var shortcutIcon by remember { mutableStateOf(IconInfo("Unknown", Icons.Default.DeviceUnknown)) }
    var buttonsList by remember { mutableStateOf(mutableListOf<Triple<String, KeyboardShortcut, String>>()) }
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
                Triple(
                    buttonEntity.buttonName,
                    KeyboardShortcut(buttonEntity.shortcutName, buttonEntity.shortcutValue),
                    "Filled.DeviceUnknown"
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

                                val text = "Deleted button: ${lastButton.first}"
                                val duration = Toast.LENGTH_SHORT

                                val toast = Toast.makeText(activity.applicationContext, text, duration)
                                toast.show()
                            }
                            else {
                                val text = "No button to delete"
                                val duration = Toast.LENGTH_SHORT

                                val toast = Toast.makeText(activity.applicationContext, text, duration)
                                toast.show()
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
                onCreateButtonClicked = { (name, shortcut, icon) ->
                    shortcutName = name
                    shortcutValue = shortcut.value
                    shortcutIcon = icon
                    // TODO : Add Icon to the button and save it in the local storage
                    println("Shortcut name: $shortcutName, Shortcut value: $shortcutValue, Shortcut icon: ${shortcutIcon.name}")
                    showPopup = false
                    val defaultButton = Triple(
                        name,
                        KeyboardShortcut(shortcutName, shortcutValue),
                        shortcutIcon.name
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

                    val text = "Button created: $name"
                    val duration = Toast.LENGTH_SHORT

                    val toast = Toast.makeText(activity.applicationContext, text, duration)
                    toast.show()
                },
                onCancelButtonClicked = {
                    showPopup = false

                    val text = "Button creation cancelled"
                    val duration = Toast.LENGTH_SHORT

                    val toast = Toast.makeText(activity.applicationContext, text, duration)
                    toast.show()
                }
            )
        }
    }
}

@Composable
fun IconDropdownMenu(
    icons: List<IconInfo>,
    onIconSelected: (ImageVector) -> Unit
) {
    var menuExpand by remember { mutableStateOf(false) }
    var selectedIcon by remember { mutableStateOf(IconInfo("Unknown", Icons.Default.DeviceUnknown).icon) }

    Column {
        // Menu of icons
        DropdownMenu(
            expanded = menuExpand,
            onDismissRequest = { menuExpand = false }
        ) {
            icons.forEach { (name, icon) ->
                Box(
                    modifier = Modifier
                        .clickable {
                            selectedIcon = icon
                            onIconSelected(icon)
                            menuExpand = false
                        }
                        .padding(8.dp)
                ) {
                    Row {
                        Icon(imageVector = icon, contentDescription = null)
                        Text(text = name)
                    }
                }
            }
        }

        Button(
            onClick = { menuExpand = true },
            colors = ButtonDefaults.buttonColors(Color.Blue)
        ) {
            Row {
                Icon(imageVector = selectedIcon, contentDescription = null)
                if (selectedIcon == Icons.Default.DeviceUnknown) {
                    Text(text = "Selected icon")
                } else {
                    Text(text = selectedIcon.name)
                }
            }
        }
    }
}

/**
 * Composable that displays a dialog to add a new shortcut button
 */
@Composable
fun DialogAddButton(
    onCreateButtonClicked: (Triple<String, KeyboardShortcut, IconInfo>) -> Unit,
    onCancelButtonClicked: () -> Unit
) {
    var enteredName by remember { mutableStateOf(TextFieldValue()) }
    var menuExpand by remember { mutableStateOf(false) }
    var shortcutSelected by remember { mutableStateOf(KeyboardShortcut("Unknown", -1)) }
    var iconSelected by remember { mutableStateOf(IconInfo("Unknown", Icons.Default.DeviceUnknown)) }
    val keyboardShortcuts = KeyboardReport.KeyEventMap.map { (keyCode, value) ->
        val name = KeyEvent.keyCodeToString(keyCode) ?: "Unknown"
        KeyboardShortcut(name, value)
    }

    AlertDialog(
        onDismissRequest = { onCancelButtonClicked() },
        title = { Text("Create a new shortcut button") },
        text = {
            Column {
                // Menu of icons
                IconDropdownMenu(
                    icons = listOf(
                        IconInfo("Home", Icons.Default.Home),
                        IconInfo("Menu", Icons.Default.Menu),
                        IconInfo("Close", Icons.Default.Close),
                        IconInfo("Arrow left", Icons.Default.ArrowBack),
                        IconInfo("Arrow down", Icons.Default.ArrowDownward),
                        IconInfo("Arrow right", Icons.Default.ArrowForward),
                        IconInfo("Arrow up", Icons.Default.ArrowUpward),
                        IconInfo("Delete", Icons.Default.Delete),
                        IconInfo("Deck", Icons.Default.Deck)
                    )
                ) { iconCurrent ->
                    println("Selected Icon: ${iconCurrent.name}, Drawable: $iconCurrent")
                    iconSelected = IconInfo(iconCurrent.name, iconCurrent)
                }
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
                        onCreateButtonClicked(Triple(enteredName.text, shortcutSelected, iconSelected))
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