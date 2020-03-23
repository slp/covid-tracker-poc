package org.covidtracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*


class ResetAlarmService : Service() {
    private var messenger: Messenger? = null
    private lateinit var wifiManager: WifiManager

    private var btLastTimestamp: Long = 0
    private var btLastResults: String = ""

    private var wifiLastTimestamp: Long = 0
    private var wifiLastResults: String = ""

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, "ct-alerts")
            .setContentTitle("COVIDTracker Service")
            .setContentText("Collecting information from nearby devices")
            .setSmallIcon(R.drawable.notification_icon)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

        messenger = intent.getParcelableExtra("messenger") as Messenger?

        val wifiIntentFilter = IntentFilter()
        wifiIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        applicationContext.registerReceiver(wifiScanReceiver, wifiIntentFilter)

        val btIntentFilter = IntentFilter()
        btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        applicationContext.registerReceiver(btScanReceiver, btIntentFilter)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val filter = IntentFilter("org.covidtracker.REFRESH_RESULTS")
        this.registerReceiver(broadcastReceiver, filter)

        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val alarmPendingIntent = PendingIntent.getBroadcast(this, 0,
            alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() +
                    10 * 1000, 10 * 60 * 1000.toLong(), alarmPendingIntent)

        return START_STICKY
    }

    private fun sendResults(results: String, kind: Int) {
        if (messenger != null) {
            val bundle = Bundle()
            when (kind) {
                WIFI_RESULTS -> bundle.putString("wifi_results", results)
                BT_RESULTS -> bundle.putString("bt_results", results)
            }

            val msg = Message.obtain()
            msg.data = bundle
            messenger!!.send(msg)
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(getString(R.string.app_name), "onReceive")
            sendResults(btLastResults, BT_RESULTS)
            sendResults(wifiLastResults, WIFI_RESULTS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val btScanReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val calendar = Calendar.getInstance()
            val timestamp = calendar.timeInMillis

            if (btLastTimestamp == 0L || (timestamp - btLastTimestamp) > 10000) {
                val formatter = SimpleDateFormat.getDateTimeInstance()
                calendar.timeInMillis = timestamp
                btLastResults = "Last BT scan at: " + formatter.format(calendar.time) + "\n"
            }

            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    val deviceHardwareAddress = device.address
                    Log.d(getString(R.string.app_name), "BT MAC: $deviceHardwareAddress")
                    btLastResults += "BT MAC: $deviceHardwareAddress\n"
                }
            }

            sendResults(wifiLastResults, WIFI_RESULTS)
        }
    }

    private val wifiScanReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val calendar = Calendar.getInstance()
            val timestamp = calendar.timeInMillis

            Log.d(getString(R.string.app_name), "scanSuccess")
            if (wifiLastTimestamp == 0L || (timestamp - wifiLastTimestamp) > 10000) {
                val formatter = SimpleDateFormat.getDateTimeInstance()
                calendar.timeInMillis = timestamp
                wifiLastResults = "Last WIFI scan at: " + formatter.format(calendar.time) + "\n"
            }

            val results = wifiManager.scanResults
            for (result in results) {
                val bssid = result.BSSID
                Log.d(getString(R.string.app_name), "WIFI SSID: $bssid" )
                wifiLastResults += "WIFI BSSID: $bssid\n"
            }

            sendResults(wifiLastResults, WIFI_RESULTS)
        }
    }

    companion object {
        private const val WIFI_RESULTS = 0
        private const val BT_RESULTS = 1
    }
}