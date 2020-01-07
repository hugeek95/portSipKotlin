package com.portsip.sipsample.util

class Session {
    var sessionID: Long
    var remote: String? = null
    var displayName: String? = null
    var hasVideo = false
    var bHold = false
    var bMute = false
    var bEarlyMedia = false
    var lineName: String? = null
    var state: CALL_STATE_FLAG
    fun IsIdle(): Boolean {
        return state == CALL_STATE_FLAG.FAILED || state == CALL_STATE_FLAG.CLOSED
    }

    fun Reset() {
        remote = null
        displayName = null
        hasVideo = false
        sessionID = INVALID_SESSION_ID.toLong()
        state = CALL_STATE_FLAG.CLOSED
        bEarlyMedia = false
    }

    enum class CALL_STATE_FLAG {
        INCOMING, TRYING, CONNECTED, FAILED, CLOSED
    }

    companion object {
        var INVALID_SESSION_ID = -1
    }

    init {
        sessionID = INVALID_SESSION_ID.toLong()
        state = CALL_STATE_FLAG.CLOSED
    }
}