package com.example.bluetoothsample

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : ComponentActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var bluetoothController: BluetoothController

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Bluetooth connection granted")
                startHomePage()
            } else {
                Log.e(TAG, "Bluetooth connection not granted, Bye!")
                finish()
            }
        }

    private fun ensureBluetoothPermission() {
        // Demander la permission BLUETOOTH_ADMIN (nécessaire pour les versions Android antérieures à S)
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADMIN)

        // Si la version Android est S ou supérieure, demander également la permission BLUETOOTH_CONNECT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Assurez-vous que la permission Bluetooth est accordée avant de continuer
        ensureBluetoothPermission()

        bluetoothController = BluetoothController()
    }

    override fun onPause() {
        super.onPause()
        bluetoothController.release()
    }

    private fun startHomePage() {
        val intent = Intent(this, HomePage::class.java)
        startActivity(intent)
    }
}
typealias KeyModifier = Int