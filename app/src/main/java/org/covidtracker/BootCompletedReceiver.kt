package org.covidtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat


class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, bootCompletedIntent: Intent) {
        if (bootCompletedIntent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context.applicationContext, ResetAlarmService::class.java)
            ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
        }
    }
}