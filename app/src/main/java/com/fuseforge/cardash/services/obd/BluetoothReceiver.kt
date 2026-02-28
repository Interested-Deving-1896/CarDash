package com.fuseforge.cardash.services.obd

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothReceiver(
    private val onBluetoothEnabled: () -> Unit,
    private val onDeviceDisconnected: (String) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d("BluetoothReceiver", "Bluetooth enabled")
                    onBluetoothEnabled()
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? = @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.address?.let { address ->
                    Log.d("BluetoothReceiver", "Device disconnected: $address")
                    onDeviceDisconnected(address)
                }
            }
        }
    }
}
