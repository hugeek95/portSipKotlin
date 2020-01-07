package com.portsip.sipsample.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PortMessageReceiver : BroadcastReceiver() {
    interface BroadcastListener {
        fun onBroadcastReceiver(intent: Intent?)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (broadcastReceiver != null) {
            broadcastReceiver!!.onBroadcastReceiver(intent)
        }
    }

    @JvmField
    var broadcastReceiver: BroadcastListener? = null
}