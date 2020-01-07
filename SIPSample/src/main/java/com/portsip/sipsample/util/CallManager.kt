package com.portsip.sipsample.util

import com.portsip.PortSIPVideoRenderer
import com.portsip.PortSipEnumDefine
import com.portsip.PortSipSdk

class CallManager private constructor() {
    var sessions: Array<Session?>
    var CurrentLine = 0
    var regist = false
    var isSpeakerOn = false
    fun setSpeakerOn(portSipSdk: PortSipSdk, speakerOn: Boolean): Boolean {
        isSpeakerOn = speakerOn
        if (speakerOn) {
            portSipSdk.setAudioDevice(PortSipEnumDefine.AudioDevice.SPEAKER_PHONE)
        } else {
            portSipSdk.setAudioDevice(PortSipEnumDefine.AudioDevice.EARPIECE)
        }
        return speakerOn
    }

    fun hangupAllCalls(sdk: PortSipSdk) {
        for (session in sessions) {
            if (session!!.sessionID > Session.INVALID_SESSION_ID) {
                sdk.hangUp(session.sessionID)
            }
        }
    }

    fun hasActiveSession(): Boolean {
        for (session in sessions) {
            if (session!!.sessionID > Session.INVALID_SESSION_ID) {
                return true
            }
        }
        return false
    }

    fun findSessionBySessionID(SessionID: Long): Session? {
        for (session in sessions) {
            if (session!!.sessionID == SessionID) {
                return session
            }
        }
        return null
    }

    fun findIdleSession(): Session? {
        for (session in sessions) {
            if (session!!.IsIdle()) {
                return session
            }
        }
        return null
    }

    val currentSession: Session?
        get() = if (CurrentLine >= 0 && CurrentLine <= sessions.size) {
            sessions[CurrentLine]
        } else null

    fun findSessionByIndex(index: Int): Session? {
        return if (index >= 0 && index <= sessions.size) {
            sessions[index]
        } else null
    }

    fun addActiveSessionToConfrence(sdk: PortSipSdk) {
        for (session in sessions) {
            if (session!!.state == Session.CALL_STATE_FLAG.CONNECTED) {
                sdk.joinToConference(session.sessionID)
                sdk.sendVideo(session.sessionID, true)
            }
        }
    }

    fun setRemoteVideoWindow(sdk: PortSipSdk, sessionid: Long, renderer: PortSIPVideoRenderer?) {
        sdk.setConferenceVideoWindow(null)
        for (session in sessions) {
            if (session!!.state == Session.CALL_STATE_FLAG.CONNECTED && sessionid != session.sessionID) {
                sdk.setRemoteVideoWindow(session.sessionID, null)
            }
        }
        sdk.setRemoteVideoWindow(sessionid, renderer)
    }

    fun setConferenceVideoWindow(sdk: PortSipSdk, renderer: PortSIPVideoRenderer?) {
        for (session in sessions) {
            if (session!!.state == Session.CALL_STATE_FLAG.CONNECTED) {
                sdk.setRemoteVideoWindow(session.sessionID, null)
            }
        }
        sdk.setConferenceVideoWindow(renderer)
    }

    fun resetAll() {
        for (session in sessions) {
            session!!.Reset()
        }
    }

    fun findIncomingCall(): Session? {
        for (session in sessions) {
            if (session!!.sessionID != Session.INVALID_SESSION_ID.toLong() && session.state == Session.CALL_STATE_FLAG.INCOMING) {
                return session
            }
        }
        return null
    }

    companion object {
        const val MAX_LINES = 10
        private var mInstance: CallManager? = null
        private val locker = Any()
        fun Instance(): CallManager? {
            if (mInstance == null) {
                synchronized(locker) {
                    if (mInstance == null) {
                        mInstance = CallManager()
                    }
                }
            }
            return mInstance
        }
    }

    init {
        sessions = arrayOfNulls(MAX_LINES)
        for (i in sessions.indices) {
            sessions[i] = Session()
            sessions[i]!!.lineName = "line - $i"
        }
    }
}