package org.covidtracker

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Handler
import android.util.Log


class AlarmReceiver : BroadcastReceiver() {
    private lateinit var wifiManager: WifiManager
    private lateinit var btManager: BluetoothManager
    private lateinit var btAdapter: BluetoothAdapter

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext

        Log.d(appContext.getString(R.string.app_name), "AlarmReciver onReceive")
        wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        btManager = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = btManager.adapter

        if (!btAdapter.startDiscovery()) {
            Log.d(appContext.getString(R.string.app_name), "BT startDiscovery failed")
        } else {
            Log.d(appContext.getString(R.string.app_name), "BT startDiscovery success")
        }

        @Suppress("DEPRECATION")
        if (!wifiManager.startScan()) {
            Log.d(appContext.getString(R.string.app_name), "WIFI startScan failed")
        } else {
            Log.d(appContext.getString(R.string.app_name), "WIFI startScan success")
        }

        Log.d(appContext.getString(R.string.app_name), "startDiscovery")
        val handler = Handler()
        handler.postDelayed({
            btAdapter.cancelDiscovery()
            Log.d(appContext.getString(R.string.app_name), "BT cancelDiscovery")
        }, 20000)
    }
}