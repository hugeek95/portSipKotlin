package com.portsip.sipsample.util

class Contact {
    var subRequestDescription: String? = null

    enum class SUBSCRIBE_STATE_FLAG {
        UNSETTLLED, ACCEPTED, REJECTED, UNSUBSCRIBE
    }

    @JvmField
    var sipAddr: String? = null
    var subDescription: String? = null
    var subScribRemote: Boolean
    var subId //if SubId >0 means received remote subscribe
            : Long
    var state // weigher accepte remote subscribe
            : SUBSCRIBE_STATE_FLAG

    fun currentStatusToString(): String {
        var status = ""
        status += "Subscribe：$subScribRemote"
        status += "  Remote presence is：$subDescription"
        status += " Subscription received:($subRequestDescription)"
        status += when (state) {
            SUBSCRIBE_STATE_FLAG.ACCEPTED -> "Accepted"
            SUBSCRIBE_STATE_FLAG.REJECTED -> "Rejected"
            SUBSCRIBE_STATE_FLAG.UNSETTLLED -> "Pending"
            SUBSCRIBE_STATE_FLAG.UNSUBSCRIBE -> "Not subscripted"
        }
        return status
    }

    init {
        state = SUBSCRIBE_STATE_FLAG.UNSUBSCRIBE //Not being subscripted
        subScribRemote = false //Not subscripted
        subId = 0
    }
}