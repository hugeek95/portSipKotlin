package com.portsip.sipsample.service

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) { //super.onMessageReceived(remoteMessage);
        val data = remoteMessage.data
        if (data != null) {
            if ("call" == data["msg_type"]) {
                val srvIntent = Intent(this, PortSipService::class.java)
                srvIntent.action = PortSipService.ACTION_PUSH_MESSAGE
                startService(srvIntent)
            }
            if ("im" == data["msg_type"]) {
                val content = data["msg_content"]
                val from = data["send_from"]
                val to = data["send_to"]
                val pushid = data["portsip-push-id"] //old
                val xpushid = data["x-push-id"] //new version
                val srvIntent = Intent(this, PortSipService::class.java)
                srvIntent.action = PortSipService.ACTION_PUSH_MESSAGE
                startService(srvIntent)
            }
        }
    }

    override fun onNewToken(s: String) {
        sendRegistrationToServer(s)
    }

    override fun onMessageSent(s: String) {
        super.onMessageSent(s)
    }

    override fun onSendError(s: String, e: Exception) {
        super.onSendError(s, e)
    }

    private fun sendRegistrationToServer(token: String) {
        val intent = Intent(this, PortSipService::class.java)
        intent.action = PortSipService.ACTION_PUSH_TOKEN
        intent.putExtra(PortSipService.EXTRA_PUSHTOKEN, token)
        startService(intent)
    }
}