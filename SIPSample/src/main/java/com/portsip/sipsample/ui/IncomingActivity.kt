package com.portsip.sipsample.ui

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.portsip.PortSipErrorcode
import com.portsip.R
import com.portsip.sipsample.receiver.PortMessageReceiver
import com.portsip.sipsample.receiver.PortMessageReceiver.BroadcastListener
import com.portsip.sipsample.service.PortSipService
import com.portsip.sipsample.util.CallManager
import com.portsip.sipsample.util.Ring
import com.portsip.sipsample.util.Session
import com.portsip.sipsample.util.Session.CALL_STATE_FLAG

/*

class IncomingActivity : Activity(), BroadcastListener, View.OnClickListener {
    var receiver: PortMessageReceiver? = null
    var application: MyApplication? = null
    var tips: TextView? = null
    var btnVideo: Button? = null
    var mSessionid: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.incomingview)
        tips = findViewById(R.id.sessiontips)
        btnVideo = findViewById(R.id.answer_video)
        receiver = PortMessageReceiver()
        val filter = IntentFilter()
        filter.addAction(PortSipService.REGISTER_CHANGE_ACTION)
        filter.addAction(PortSipService.CALL_CHANGE_ACTION)
        filter.addAction(PortSipService.PRESENCE_CHANGE_ACTION)
        registerReceiver(receiver, filter)
        receiver!!.broadcastReceiver = this
        val intent = intent
        mSessionid = intent.getLongExtra("incomingSession", PortSipErrorcode.INVALID_SESSION_ID.toLong())
        val session = CallManager.Instance().findSessionBySessionID(mSessionid)
        if (mSessionid == PortSipErrorcode.INVALID_SESSION_ID.toLong() || session == null || session.state != CALL_STATE_FLAG.INCOMING) {
            finish()
        }
        application = getApplication() as MyApplication
        tips.setText(session!!.lineName + "   " + session.remote)
        setVideoAnswerVisibility(session)
        findViewById<View>(R.id.hangup_call).setOnClickListener(this)
        findViewById<View>(R.id.answer_audio).setOnClickListener(this)
        btnVideo.setOnClickListener(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val sessionid = intent.getLongExtra("incomingSession", PortSipErrorcode.INVALID_SESSION_ID.toLong())
        val session = CallManager.Instance().findSessionBySessionID(sessionid)
        if (mSessionid != PortSipErrorcode.INVALID_SESSION_ID.toLong() && session != null) {
            mSessionid = sessionid
            setVideoAnswerVisibility(session)
            tips!!.text = session.lineName + "   " + session.remote
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onBroadcastReceiver(intent: Intent) {
        val action = intent.action
        if (PortSipService.CALL_CHANGE_ACTION == action) {
            val sessionId = intent.getLongExtra(PortSipService.EXTRA_CALL_SEESIONID, Session.INVALID_SESSION_ID.toLong())
            val status = intent.getStringExtra(PortSipService.EXTRA_CALL_DESCRIPTION)
            val session = CallManager.Instance().findSessionBySessionID(sessionId)
            if (session != null) {
                when (session.state) {
                    CALL_STATE_FLAG.INCOMING -> {
                    }
                    CALL_STATE_FLAG.TRYING -> {
                    }
                    CALL_STATE_FLAG.CONNECTED, CALL_STATE_FLAG.FAILED, CALL_STATE_FLAG.CLOSED -> {
                        val anOthersession = CallManager.Instance().findIncomingCall()
                        if (anOthersession == null) {
                            finish()
                        } else {
                            setVideoAnswerVisibility(anOthersession)
                            tips!!.text = anOthersession.lineName + "   " + anOthersession.remote
                            mSessionid = anOthersession.sessionID
                        }
                    }
                }
            }
        }
    }

    override fun onClick(view: View) {
        if (application!!.mEngine != null) {
            val currentLine = CallManager.Instance().findSessionBySessionID(mSessionid)
            when (view.id) {
                R.id.answer_audio, R.id.answer_video -> {
                    if (currentLine.state != CALL_STATE_FLAG.INCOMING) {
                        Toast.makeText(this, currentLine.lineName + "No incoming call on current line", Toast.LENGTH_SHORT)
                        return
                    }
                    Ring.getInstance(this).stopRingTone()
                    currentLine.state = CALL_STATE_FLAG.CONNECTED
                    application!!.mEngine.answerCall(mSessionid, view.id == R.id.answer_video)
                    if (application!!.mConference) {
                        application!!.mEngine.joinToConference(currentLine.sessionID)
                    }
                }
                R.id.hangup_call -> {
                    Ring.getInstance(this).stop()
                    if (currentLine.state == CALL_STATE_FLAG.INCOMING) {
                        application!!.mEngine.rejectCall(currentLine.sessionID, 486)
                        currentLine.Reset()
                        Toast.makeText(this, currentLine.lineName + ": Rejected call", Toast.LENGTH_SHORT)
                    }
                }
            }
        }
        val anOthersession = CallManager.Instance().findIncomingCall()
        if (anOthersession == null) {
            finish()
        } else {
            mSessionid = anOthersession.sessionID
            setVideoAnswerVisibility(anOthersession)
        }
    }

    private fun setVideoAnswerVisibility(session: Session?) {
        if (session == null) return
        if (session.hasVideo) {
            btnVideo!!.visibility = View.VISIBLE
        } else {
            btnVideo!!.visibility = View.GONE
        }
    }
}*/