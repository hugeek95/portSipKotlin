package com.portsip.sipsample.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.portsip.*
//import com.portsip.sipsample.ui.IncomingActivity
import com.portsip.sipsample.ui.MainActivity
import com.portsip.sipsample.ui.MyApplication
import com.portsip.sipsample.util.*
import com.portsip.sipsample.util.Contact.SUBSCRIBE_STATE_FLAG
import java.util.*

class PortSipService : Service(), OnPortSIPEvent {
    protected var mCpuLock: PowerManager.WakeLock? = null
    private val APPID = "com.portsip.sipsample"
    private var mEngine: PortSipSdk? = null
    private var applicaton: MyApplication? = null
    private val SERVICE_NOTIFICATION = 31414
    private val channelID = "PortSipService"
    private var pushToken: String? = null
    override fun onCreate() {
        super.onCreate()
        applicaton = applicationContext as MyApplication
        mEngine = applicaton!!.mEngine
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableLights(true)
            notificationManager.createNotificationChannel(channel)
        }
        showServiceNotifiCation()
        try {
            FirebaseInstanceId.getInstance().instanceId
                    .addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            return@OnCompleteListener
                        }
                        pushToken = task.result!!.token
                        refreshPushToken()
                    })
        } catch (e: IllegalStateException) {
            Log.d("", e.toString())
        }
    }

    private fun showServiceNotifiCation() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        val contentIntent = PendingIntent.getActivity(this, 0 /*requestCode*/, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder: Notification.Builder
        builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelID)
        } else {
            Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Service Running")
                .setContentIntent(contentIntent)
                .build() // getNotification()
        startForeground(SERVICE_NOTIFICATION, builder.build())
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            if (ACTION_PUSH_MESSAGE == intent.action && !CallManager.Instance()!!.regist) {
                registerToServer()
            } else if (ACTION_SIP_REGIEST == intent.action && !CallManager.Instance()!!.regist) {
                registerToServer()
            } else if (ACTION_SIP_UNREGIEST == intent.action && CallManager.Instance()!!.regist) {
                unregisterToServer()
            } else if (ACTION_PUSH_TOKEN == intent.action) {
                pushToken = intent.getStringExtra(EXTRA_PUSHTOKEN)
                refreshPushToken()
            }
        }
        return result
    }

    private fun refreshPushToken() {
        if (!TextUtils.isEmpty(pushToken)) {
            val pushMessage = "device-os=android;device-uid=$pushToken;allow-call-push=true;allow-message-push=true;app-id=$APPID"
            //old version
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "portsip-push", pushMessage)
            //new version
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "x-p-push", pushMessage)
            mEngine!!.refreshRegistration(0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mEngine!!.destroyConference()
        if (mCpuLock != null) {
            mCpuLock!!.release()
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(channelID)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return null!!
    }

    fun registerToServer() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val rm = Random()
        val localPort = 5060 + rm.nextInt(60000)
        val transType = preferences.getInt(TRANS, 0)
        val srtpType = preferences.getInt(SRTP, 0)
        val userName = preferences.getString(USER_NAME, "")
        val password = preferences.getString(USER_PWD, "")
        val displayName = preferences.getString(USER_DISPALYNAME, "")
        val authName = preferences.getString(USER_AUTHNAME, "")
        val userDomain = preferences.getString(USER_DOMAIN, "")
        val sipServer = preferences.getString(SVR_HOST, "")
        val serverPort = preferences.getString(SVR_PORT, "5060")
        val stunServer = preferences.getString(STUN_HOST, "")
        val stunPort = preferences.getString(STUN_PORT, "3478")
        val sipServerPort = serverPort.toInt()
        val stunServerPort = stunPort.toInt()
        if (TextUtils.isEmpty(userName)) {
            showTipMessage("Please enter user name!")
            return
        }
        if (TextUtils.isEmpty(password)) {
            showTipMessage("Please enter password!")
            return
        }
        if (TextUtils.isEmpty(sipServer)) {
            showTipMessage("Please enter SIP Server!")
            return
        }
        if (TextUtils.isEmpty(serverPort)) {
            showTipMessage("Please enter Server Port!")
            return
        }
        var result = 0
        mEngine!!.DeleteCallManager()
        mEngine!!.CreateCallManager(applicaton)
        mEngine!!.setOnPortSIPEvent(this)
        val dataPath = getExternalFilesDir(null).absolutePath
        result = mEngine!!.initialize(getTransType(transType), "0.0.0.0", localPort,
                PortSipEnumDefine.ENUM_LOG_LEVEL_DEBUG, dataPath,
                8, "PortSIP SDK for Android", 0, 0, dataPath, "", false, null)
        if (result != PortSipErrorcode.ECoreErrorNone) {
            showTipMessage("initialize failure ErrorCode = $result")
            mEngine!!.DeleteCallManager()
            CallManager.Instance()!!.resetAll()
            return
        }
        result = mEngine!!.setUser(userName, displayName, authName, password,
                userDomain, sipServer, sipServerPort, stunServer, stunServerPort, sipServer, 56749)
        if (result != PortSipErrorcode.ECoreErrorNone) {
            showTipMessage("setUser failure ErrorCode = $result")
            mEngine!!.DeleteCallManager()
            CallManager.Instance()!!.resetAll()
            return
        }
        result = mEngine!!.setLicenseKey("LicenseKey")
        if (result == PortSipErrorcode.ECoreWrongLicenseKey) {
            showTipMessage("The wrong license key was detected, please check with sales@portsip.com or support@portsip.com")
            return
        } else if (result == PortSipErrorcode.ECoreTrialVersionLicenseKey) {
            Log.w("Trial Version", "This trial version SDK just allows short conversation, you can't hearing anything after 2-3 minutes, contact us: sales@portsip.com to buy official version.")
            showTipMessage("This Is Trial Version")
        }
        mEngine!!.setAudioDevice(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE)
        mEngine!!.setVideoDeviceId(1)
        mEngine!!.setSrtpPolicy(srtpType)
        ConfigPreferences(this, preferences, mEngine)
        mEngine!!.enable3GppTags(false)
        if (!TextUtils.isEmpty(pushToken)) {
            val pushMessage = "device-os=android;device-uid=$pushToken;allow-call-push=true;allow-message-push=true;app-id=$APPID"
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "portsip-push", pushMessage)
            //new version
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "x-p-push", pushMessage)
        }
        mEngine!!.setInstanceId(instanceID)
        result = mEngine!!.registerServer(90, 0)
        if (result != PortSipErrorcode.ECoreErrorNone) {
            showTipMessage("registerServer failure ErrorCode =$result")
            mEngine!!.unRegisterServer()
            mEngine!!.DeleteCallManager()
            CallManager.Instance()!!.resetAll()
        }
    }

    private fun showTipMessage(tipMessage: String) {
        val broadIntent = Intent(REGISTER_CHANGE_ACTION)
        broadIntent.putExtra(EXTRA_REGISTER_STATE, tipMessage)
        sendPortSipMessage(tipMessage, broadIntent)
    }

    private fun getTransType(select: Int): Int {
        when (select) {
            0 -> return PortSipEnumDefine.ENUM_TRANSPORT_UDP
            1 -> return PortSipEnumDefine.ENUM_TRANSPORT_TLS
            2 -> return PortSipEnumDefine.ENUM_TRANSPORT_TCP
            3 -> return PortSipEnumDefine.ENUM_TRANSPORT_PERS_UDP
            4 -> return PortSipEnumDefine.ENUM_TRANSPORT_PERS_TCP
        }
        return PortSipEnumDefine.ENUM_TRANSPORT_UDP
    }

    val instanceID: String
        get() {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            var insanceid = preferences.getString(INSTANCE_ID, "")
            if (TextUtils.isEmpty(insanceid)) {
                insanceid = UUID.randomUUID().toString()
                preferences.edit().putString(INSTANCE_ID, insanceid).commit()
            }
            return insanceid
        }

    fun UnregisterToServerWithoutPush() {
        if (!TextUtils.isEmpty(pushToken)) {
            val pushMessage = "device-os=android;device-uid=$pushToken;allow-call-push=false;allow-message-push=false;app-id=$APPID"
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "portsip-push", pushMessage)
            //new version
            mEngine!!.addSipMessageHeader(-1, "REGISTER", 1, "x-p-push", pushMessage)
        }
        mEngine!!.unRegisterServer()
        mEngine!!.DeleteCallManager()
        CallManager.Instance()!!.regist = false
    }

    fun unregisterToServer() {
        mEngine!!.unRegisterServer()
        mEngine!!.DeleteCallManager()
        CallManager.Instance()!!.regist = false
    }

    //--------------------
    override fun onRegisterSuccess(statusText: String, statusCode: Int, sipMessage: String) {
        CallManager.Instance()!!.regist = true
        val broadIntent = Intent(REGISTER_CHANGE_ACTION)
        broadIntent.putExtra(EXTRA_REGISTER_STATE, statusText)
        sendPortSipMessage("onRegisterSuccess", broadIntent)
        keepCpuRun(true)
    }

    override fun onRegisterFailure(statusText: String, statusCode: Int, sipMessage: String) {
        val broadIntent = Intent(REGISTER_CHANGE_ACTION)
        broadIntent.putExtra(EXTRA_REGISTER_STATE, statusText)
        sendPortSipMessage("onRegisterFailure$statusCode", broadIntent)
        CallManager.Instance()!!.regist = false
        CallManager.Instance()!!.resetAll()
        keepCpuRun(false)
    }

    override fun onInviteIncoming(sessionId: Long,
                                  callerDisplayName: String,
                                  caller: String,
                                  calleeDisplayName: String,
                                  callee: String,
                                  audioCodecNames: String,
                                  videoCodecNames: String,
                                  existsAudio: Boolean,
                                  existsVideo: Boolean,
                                  sipMessage: String) {
        if (CallManager.Instance()!!.findIncomingCall() != null) {
            applicaton!!.mEngine!!.rejectCall(sessionId, 486) //busy
            return
        }
        val session = CallManager.Instance()!!.findIdleSession()
        session!!.state = Session.CALL_STATE_FLAG.INCOMING
        session!!.hasVideo = existsVideo
        session!!.sessionID = sessionId
        session!!.remote = caller
        session!!.displayName = callerDisplayName
       /* val activityIntent = Intent(this, IncomingActivity::class.java)
        activityIntent.putExtra("incomingSession", sessionId)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(activityIntent)*/
        val broadIntent = Intent(CALL_CHANGE_ACTION)
        broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
        val description = session.lineName + " onInviteIncoming"
        broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
        sendPortSipMessage(description, broadIntent)
        Ring.getInstance(this)!!.startRingTone()
    }

    override fun onInviteTrying(sessionId: Long) {}
    override fun onInviteSessionProgress(
            sessionId: Long,
            audioCodecNames: String,
            videoCodecNames: String,
            existsEarlyMedia: Boolean,
            existsAudio: Boolean,
            existsVideo: Boolean,
            sipMessage: String) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.bEarlyMedia = existsEarlyMedia
        }
    }

    override fun onInviteRinging(sessionId: Long, statusText: String, statusCode: Int, sipMessage: String) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null && !session.bEarlyMedia) {
            Ring.getInstance(this)!!.startRingBackTone()
        }
    }

    override fun onInviteAnswered(sessionId: Long,
                                  callerDisplayName: String,
                                  caller: String,
                                  calleeDisplayName: String,
                                  callee: String,
                                  audioCodecNames: String,
                                  videoCodecNames: String,
                                  existsAudio: Boolean,
                                  existsVideo: Boolean,
                                  sipMessage: String) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED
            session.hasVideo = existsVideo
            val broadIntent = Intent(CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " onInviteAnswered"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
        Ring.getInstance(this)!!.stopRingBackTone()
    }

    override fun onInviteFailure(sessionId: Long, reason: String, code: Int, sipMessage: String) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.FAILED
            session.sessionID = sessionId
            val broadIntent = Intent(CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " onInviteFailure"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
        Ring.getInstance(this)!!.stopRingBackTone()
    }

    override fun onInviteUpdated(
            sessionId: Long,
            audioCodecNames: String,
            videoCodecNames: String,
            existsAudio: Boolean,
            existsVideo: Boolean,
            sipMessage: String) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED
            session.hasVideo = existsVideo
            val broadIntent = Intent(CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " OnInviteUpdated"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
    }

    override fun onInviteConnected(sessionId: Long) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CONNECTED
            session.sessionID = sessionId
            if (applicaton!!.mConference) {
                applicaton!!.mEngine!!.joinToConference(session.sessionID)
                applicaton!!.mEngine!!.sendVideo(session.sessionID, true)
            }
            val broadIntent = Intent(CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " OnInviteConnected"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
        CallManager.Instance()!!.setSpeakerOn(applicaton!!.mEngine!!, CallManager.Instance()!!.isSpeakerOn)
    }

    override fun onInviteBeginingForward(forwardTo: String) {}
    override fun onInviteClosed(sessionId: Long) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED
            session.sessionID = sessionId
            val broadIntent = Intent(CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " OnInviteClosed"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
        Ring.getInstance(this)!!.stopRingTone()
    }

    override fun onDialogStateUpdated(BLFMonitoredUri: String,
                                      BLFDialogState: String,
                                      BLFDialogId: String,
                                      BLFDialogDirection: String) {
        var text = "The user "
        text += BLFMonitoredUri
        text += " dialog state is updated: "
        text += BLFDialogState
        text += ", dialog id: "
        text += BLFDialogId
        text += ", direction: "
        text += BLFDialogDirection
    }

    override fun onRemoteUnHold(
            sessionId: Long,
            audioCodecNames: String,
            videoCodecNames: String,
            existsAudio: Boolean,
            existsVideo: Boolean) {
    }

    override fun onRemoteHold(sessionId: Long) {}
    override fun onReceivedRefer(
            sessionId: Long,
            referId: Long,
            to: String,
            referFrom: String,
            referSipMessage: String) {
    }

    override fun onReferAccepted(sessionId: Long) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED
            session.sessionID = sessionId
            val broadIntent = Intent(CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " onReferAccepted"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
        Ring.getInstance(this)!!.stopRingTone()
    }

    override fun onReferRejected(sessionId: Long, reason: String, code: Int) {}
    override fun onTransferTrying(sessionId: Long) {}
    override fun onTransferRinging(sessionId: Long) {}
    override fun onACTVTransferSuccess(sessionId: Long) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            session.state = Session.CALL_STATE_FLAG.CLOSED
            session.sessionID = sessionId
            val broadIntent = Intent(CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " Transfer succeeded, call closed"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
            // Close the call after succeeded transfer the call
            mEngine!!.hangUp(sessionId)
        }
    }

    override fun onACTVTransferFailure(sessionId: Long, reason: String, code: Int) {
        val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
        if (session != null) {
            val broadIntent = Intent(CALL_CHANGE_ACTION)
            broadIntent.putExtra(EXTRA_CALL_SEESIONID, sessionId)
            val description = session.lineName + " Transfer failure!"
            broadIntent.putExtra(EXTRA_CALL_DESCRIPTION, description)
            sendPortSipMessage(description, broadIntent)
        }
    }

    override fun onReceivedSignaling(sessionId: Long, signaling: String) {}
    override fun onSendingSignaling(sessionId: Long, signaling: String) {}
    override fun onWaitingVoiceMessage(
            messageAccount: String,
            urgentNewMessageCount: Int,
            urgentOldMessageCount: Int,
            newMessageCount: Int,
            oldMessageCount: Int) {
    }

    override fun onWaitingFaxMessage(
            messageAccount: String,
            urgentNewMessageCount: Int,
            urgentOldMessageCount: Int,
            newMessageCount: Int,
            oldMessageCount: Int) {
    }

    override fun onRecvDtmfTone(sessionId: Long, tone: Int) {}
    override fun onRecvOptions(optionsMessage: String) {}
    override fun onRecvInfo(infoMessage: String) {}
    override fun onRecvNotifyOfSubscription(sessionId: Long, notifyMessage: String, messageData: ByteArray, messageDataLength: Int) {}
    //Receive a new subscribe
    override fun onPresenceRecvSubscribe(
            subscribeId: Long,
            fromDisplayName: String,
            from: String,
            subject: String) {
        var contact = ContactManager.Instance()!!.findContactBySipAddr(from)
        if (contact == null) {
            contact = Contact()
            contact.sipAddr = from
            ContactManager.Instance()!!.addContact(contact)
        }
        contact.subRequestDescription = subject
        contact.subId = subscribeId
        when (contact.state) {
            SUBSCRIBE_STATE_FLAG.ACCEPTED -> applicaton!!.mEngine!!.presenceAcceptSubscribe(subscribeId)
            SUBSCRIBE_STATE_FLAG.REJECTED -> applicaton!!.mEngine!!.presenceRejectSubscribe(subscribeId)
            SUBSCRIBE_STATE_FLAG.UNSETTLLED -> {
            }
            SUBSCRIBE_STATE_FLAG.UNSUBSCRIBE -> contact.state = SUBSCRIBE_STATE_FLAG.UNSETTLLED
        }
        val broadIntent = Intent(PRESENCE_CHANGE_ACTION)
        sendPortSipMessage("OnPresenceRecvSubscribe", broadIntent)
    }

    //update online status
    override fun onPresenceOnline(fromDisplayName: String, from: String, stateText: String) {
        val contact = ContactManager.Instance()!!.findContactBySipAddr(from)
        if (contact == null) {
        } else {
            contact.subDescription = stateText
        }
        val broadIntent = Intent(PRESENCE_CHANGE_ACTION)
        sendPortSipMessage("OnPresenceRecvSubscribe", broadIntent)
    }

    //update offline status
    override fun onPresenceOffline(fromDisplayName: String, from: String) {
        val contact = ContactManager.Instance()!!.findContactBySipAddr(from)
        if (contact == null) {
        } else {
            contact.subDescription = "Offline"
        }
        val broadIntent = Intent(PRESENCE_CHANGE_ACTION)
        sendPortSipMessage("OnPresenceRecvSubscribe", broadIntent)
    }

    override fun onRecvMessage(
            sessionId: Long,
            mimeType: String,
            subMimeType: String,
            messageData: ByteArray,
            messageDataLength: Int) {
    }

    override fun onRecvOutOfDialogMessage(
            fromDisplayName: String,
            from: String,
            toDisplayName: String,
            to: String,
            mimeType: String,
            subMimeType: String,
            messageData: ByteArray,
            messageDataLengthsipMessage: Int,
            sipMessage: String) {
        if ("text" == mimeType && "plain" == subMimeType) {
            Toast.makeText(this, "you have a mesaage from: " + from + "  " + String(messageData), Toast.LENGTH_SHORT).show()
        } else {
        }
    }

    override fun onSendMessageSuccess(sessionId: Long, messageId: Long) {}
    override fun onSendMessageFailure(sessionId: Long, messageId: Long, reason: String, code: Int) {}
    override fun onSendOutOfDialogMessageSuccess(messageId: Long,
                                                 fromDisplayName: String,
                                                 from: String,
                                                 toDisplayName: String,
                                                 to: String) {
    }

    override fun onSendOutOfDialogMessageFailure(
            messageId: Long,
            fromDisplayName: String,
            from: String,
            toDisplayName: String,
            to: String,
            reason: String,
            code: Int) {
    }

    override fun onSubscriptionFailure(subscribeId: Long, statusCode: Int) {}
    override fun onSubscriptionTerminated(subscribeId: Long) {}
    override fun onPlayAudioFileFinished(sessionId: Long, fileName: String) {}
    override fun onPlayVideoFileFinished(sessionId: Long) {}
    override fun onReceivedRTPPacket(
            sessionId: Long,
            isAudio: Boolean,
            RTPPacket: ByteArray,
            packetSize: Int) {
    }

    override fun onSendingRTPPacket(l: Long, b: Boolean, bytes: ByteArray, i: Int) {}
    override fun onAudioRawCallback(
            sessionId: Long,
            callbackType: Int,
            data: ByteArray,
            dataLength: Int,
            samplingFreqHz: Int) {
    }

    override fun onVideoRawCallback(l: Long, i: Int, i1: Int, i2: Int, bytes: ByteArray, i3: Int) {}
    //--------------------
    fun sendPortSipMessage(message: String?, broadIntent: Intent?) {
        val mNotifyMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val builder: Notification.Builder
        builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelID)
        } else {
            Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.icon)
                .setContentTitle("Sip Notify")
                .setContentText(message)
                .setContentIntent(contentIntent)
                .build() // getNotification()
        mNotifyMgr.notify(1, builder.build())
        sendBroadcast(broadIntent)
    }

    fun outOfDialogRefer(replaceSessionId: Int, replaceMethod: String?, target: String?, referTo: String?): Int {
        return 0
    }

    fun keepCpuRun(keepRun: Boolean) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (keepRun == true) { //open
            if (mCpuLock == null) {
                if (powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SipSample:CpuLock.").also { mCpuLock = it } == null) {
                    return
                }
                mCpuLock!!.setReferenceCounted(false)
            }
            synchronized(mCpuLock!!) {
                if (!mCpuLock!!.isHeld) {
                    mCpuLock!!.acquire()
                }
            }
        } else { //close
            if (mCpuLock != null) {
                synchronized(mCpuLock!!) {
                    if (mCpuLock!!.isHeld) {
                        mCpuLock!!.release()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_SIP_REGIEST = "PortSip.AndroidSample.Test.REGIEST"
        const val ACTION_SIP_UNREGIEST = "PortSip.AndroidSample.Test.UNREGIEST"
        const val ACTION_PUSH_MESSAGE = "PortSip.AndroidSample.Test.PushMessageIncoming"
        const val ACTION_PUSH_TOKEN = "PortSip.AndroidSample.Test.PushToken"
        const val INSTANCE_ID = "instanceid"
        const val USER_NAME = "user name"
        const val USER_PWD = "user pwd"
        const val SVR_HOST = "svr host"
        const val SVR_PORT = "svr port"
        const val USER_DOMAIN = "user domain"
        const val USER_DISPALYNAME = "user dispalay"
        const val USER_AUTHNAME = "user authname"
        const val STUN_HOST = "stun host"
        const val STUN_PORT = "stun port"
        const val TRANS = "trans type"
        const val SRTP = "srtp type"
        const val REGISTER_CHANGE_ACTION = "PortSip.AndroidSample.Test.RegisterStatusChagnge"
        const val CALL_CHANGE_ACTION = "PortSip.AndroidSample.Test.CallStatusChagnge"
        const val PRESENCE_CHANGE_ACTION = "PortSip.AndroidSample.Test.PRESENCEStatusChagnge"
        const val EXTRA_REGISTER_STATE = "RegisterStatus"
        const val EXTRA_CALL_SEESIONID = "SessionID"
        const val EXTRA_CALL_DESCRIPTION = "Description"
        const val EXTRA_PUSHTOKEN = "token"
        @JvmStatic
        fun ConfigPreferences(context: Context, preferences: SharedPreferences, sdk: PortSipSdk?) {
            sdk!!.clearAudioCodec()
            if (preferences.getBoolean(context.getString(R.string.MEDIA_G722), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G722)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_PCMA), true)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMA)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_PCMU), true)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_PCMU)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_G729), true)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G729)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_GSM), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_GSM)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_ILBC), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ILBC)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_AMR), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_AMR)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_AMRWB), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_AMRWB)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_SPEEX), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_SPEEX)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_SPEEXWB), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_SPEEXWB)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_ISACWB), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ISACWB)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_ISACSWB), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_ISACSWB)
            }
            //        if (preferences.getBoolean(context.getString(R.string.MEDIA_G7221), false)) {
//            sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_G7221);
//        }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_OPUS), false)) {
                sdk.addAudioCodec(PortSipEnumDefine.ENUM_AUDIOCODEC_OPUS)
            }
            sdk.clearVideoCodec()
            if (preferences.getBoolean(context.getString(R.string.MEDIA_H264), true)) {
                sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_H264)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_VP8), true)) {
                sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP8)
            }
            if (preferences.getBoolean(context.getString(R.string.MEDIA_VP9), true)) {
                sdk.addVideoCodec(PortSipEnumDefine.ENUM_VIDEOCODEC_VP9)
            }
            sdk.enableAEC(preferences.getBoolean(context.getString(R.string.MEDIA_AEC), true))
            sdk.enableAGC(preferences.getBoolean(context.getString(R.string.MEDIA_AGC), true))
            sdk.enableCNG(preferences.getBoolean(context.getString(R.string.MEDIA_CNG), true))
            sdk.enableVAD(preferences.getBoolean(context.getString(R.string.MEDIA_VAD), true))
            sdk.enableANS(preferences.getBoolean(context.getString(R.string.MEDIA_ANS), false))
            val foward = preferences.getBoolean(context.getString(R.string.str_fwopenkey), false)
            val fowardBusy = preferences.getBoolean(context.getString(R.string.str_fwbusykey), false)
            val fowardto = preferences.getString(context.getString(R.string.str_fwtokey), null)
            if (foward && !TextUtils.isEmpty(fowardto)) {
                sdk.enableCallForward(fowardBusy, fowardto)
            }
            sdk.enableReliableProvisional(preferences.getBoolean(context.getString(R.string.str_pracktitle), false))
            val resolution = preferences.getString(context.getString(R.string.str_resolution), "CIF")
            var width = 352
            var height = 288
            if (resolution == "QCIF") {
                width = 176
                height = 144
            } else if (resolution == "CIF") {
                width = 352
                height = 288
            } else if (resolution == "VGA") {
                width = 640
                height = 480
            } else if (resolution == "720P") {
                width = 1280
                height = 720
            } else if (resolution == "1080P") {
                width = 1920
                height = 1080
            }
            sdk.setVideoResolution(width, height)
        }
    }
}