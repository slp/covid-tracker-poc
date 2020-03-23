package org.covidtracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    private lateinit var btResultsText: TextView
    private lateinit var wifiResultsText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btResultsText = findViewById(R.id.bt_results_text)
        wifiResultsText = findViewById(R.id.wifi_results_text)

        createNotificationChannel()

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_FINE_LOCATION)
        } else {
            resetAlarm()
        }

        val intent = Intent()
        intent.action = "org.covidtracker.REFRESH_RESULTS"
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "ct-alerts",
                "COVIDTracker Alerts Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun resetAlarm() {
        val messenger = Messenger(messageHandler)
        val serviceIntent = Intent(this, ResetAlarmService::class.java)
        serviceIntent.putExtra("messenger", messenger)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_FINE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(getString(R.string.app_name), "Location permission granted")
                    resetAlarm()
                } else {
                    Log.d(getString(R.string.app_name), "Location permission denied")
                    ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            PERMISSION_FINE_LOCATION)
                }
                return
            }
            else -> {
            }
        }
    }

    private val messageHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            val bundle = msg.data
            val btResults = bundle["bt_results"] as String?
            if (btResults != null) {
                btResultsText.text = btResults
            }
            val wifiResults = bundle["wifi_results"] as String?
            if (wifiResults != null) {
                wifiResultsText.text = wifiResults
            }
        }
    }

    companion object {
        private const val PERMISSION_FINE_LOCATION = 0
    }
}
