package com.portsip.sipsample.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import com.portsip.PortSipEnumDefine
import com.portsip.R
import com.portsip.sipsample.receiver.PortMessageReceiver.BroadcastListener
import com.portsip.sipsample.service.PortSipService
import com.portsip.sipsample.util.CallManager
import com.portsip.sipsample.util.Ring
import com.portsip.sipsample.util.Session
import com.portsip.sipsample.util.Session.CALL_STATE_FLAG

class NumpadFragment : BaseFragment(), OnItemSelectedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener, BroadcastListener {
    private var etSipNum: EditText? = null
    private var mtips: TextView? = null
    private var spline: Spinner? = null
    var cbSendVideo: CheckBox? = null
    var cbRecvVideo: CheckBox? = null
    var cbConference: CheckBox? = null
    var cbSendSdp: CheckBox? = null
    var application: MyApplication? = null
    var activity: MainActivity? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View {
        activity = getActivity() as MainActivity
        application = activity!!.applicationContext as MyApplication
        return inflater.inflate(R.layout.numpad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle) {
        super.onViewCreated(view, savedInstanceState)
        etSipNum = view.findViewById(R.id.etsipaddress)
        cbSendSdp = view.findViewById(R.id.sendSdp)
        cbConference = view.findViewById(R.id.conference)
        cbSendVideo = view.findViewById(R.id.sendVideo)
        cbRecvVideo = view.findViewById(R.id.acceptVideo)
        spline = view.findViewById(R.id.sp_lines)
        val dtmfPad = view.findViewById<TableLayout>(R.id.dtmf_pad)
        val functionPad = view.findViewById<TableLayout>(R.id.function_pad)
        val spinnerAdapter: ArrayAdapter<*> = ArrayAdapter.createFromResource(getActivity(), R.array.lines, android.R.layout.simple_list_item_1)
        spline!!.setAdapter(spinnerAdapter)
        spline!!.setSelection(CallManager.Instance()!!.CurrentLine)
        spline!!.setOnItemSelectedListener(this)
        view.findViewById<View>(R.id.dial).setOnClickListener(this)
        view.findViewById<View>(R.id.pad).setOnClickListener(this)
        view.findViewById<View>(R.id.delete).setOnClickListener(this)
        mtips = view.findViewById<View>(R.id.txtips) as TextView
      //  cbConference.setChecked(application!!.mConference)
      //  cbConference.setOnCheckedChangeListener(this)
        SetTableItemClickListener(functionPad)
        SetTableItemClickListener(dtmfPad)
        onHiddenChanged(false)
    }

    override fun onBroadcastReceiver(intent: Intent?) {
        val action = if (intent == null) "" else intent.action
        if (PortSipService.CALL_CHANGE_ACTION == action) {
            val sessionId = intent!!.getLongExtra(PortSipService.EXTRA_CALL_SEESIONID, Session.INVALID_SESSION_ID.toLong())
            val status = intent.getStringExtra(PortSipService.EXTRA_CALL_DESCRIPTION)
            showTips(status)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            ShowCurrentLineState()
            activity!!.receiver!!.broadcastReceiver = this
        }
    }

    private fun ShowCurrentLineState() {
        val current = CallManager.Instance()!!.currentSession
        when (current!!.state) {
            CALL_STATE_FLAG.CLOSED, CALL_STATE_FLAG.FAILED -> showTips(current!!.lineName + ": Idle")
            CALL_STATE_FLAG.CONNECTED -> showTips(current!!.lineName + ": CONNECTED")
            CALL_STATE_FLAG.INCOMING -> showTips(current!!.lineName + ": INCOMING")
            CALL_STATE_FLAG.TRYING -> showTips(current!!.lineName + ": TRYING")
        }
        val mute = view.findViewById<Button>(R.id.mute)
        if (current!!.bMute) {
            mute.text = "Mute"
        } else {
            mute.text = "UnMute"
        }
        val mic = view.findViewById<Button>(R.id.mic)
        if (CallManager.Instance()!!.isSpeakerOn) {
            mic.text = "SpeakOn"
        } else {
            mic.text = "SpeakOff"
        }
    }

    private fun SetTableItemClickListener(table: TableLayout) {
        for (i in 0 until table.childCount) {
            val tableRow = table.getChildAt(i) as TableRow
            val line = tableRow.childCount
            for (index in 0 until line) {
                tableRow.getChildAt(index).setOnClickListener(this)
            }
        }
    }

    private fun showTips(text: String) {
        mtips!!.text = text
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show()
    }

    private fun SwitchPanel() {
        val view = view.findViewById<View>(R.id.function_pad)
        val dtmfView = getView().findViewById<View>(R.id.dtmf_pad)
        if (view.visibility == View.VISIBLE) {
            view.visibility = View.INVISIBLE
            dtmfView.visibility = View.VISIBLE
        } else {
            view.visibility = View.VISIBLE
            dtmfView.visibility = View.INVISIBLE
        }
    }

    var TransferDLG: AlertDialog? = null
    var AttendTransferDLG: AlertDialog? = null
    private fun showTransferDialog() {
        val builder = AlertDialog.Builder(getActivity())
        val factory = LayoutInflater.from(getActivity())
        val textEntryView = factory.inflate(R.layout.transferdialog, null)
        builder.setIcon(R.drawable.icon)
        builder.setTitle("Transfer input")
        builder.setView(textEntryView)
        builder.setPositiveButton("ok") { dialogInterface, i -> handleTransfer() }
        builder.setNegativeButton("cancel") { dialogInterface, i -> handleNegativeButtonClick() }
        TransferDLG = builder.create()
        TransferDLG!!.show()
    }

    private fun showAttendTransferDialog() {
        val builder = AlertDialog.Builder(getActivity())
        val factory = LayoutInflater.from(getActivity())
        val textEntryView = factory.inflate(R.layout.attendtransferdialog, null)
        builder.setIcon(R.drawable.icon)
        builder.setTitle("Transfer input")
        builder.setView(textEntryView)
        builder.setPositiveButton("ok") { dialogInterface, i -> handleAttendTransfer() }
        builder.setNegativeButton("cancel") { dialogInterface, i -> handleNegativeButtonClick() }
        AttendTransferDLG = builder.create()
        AttendTransferDLG!!.show()
    }

    private fun handleNegativeButtonClick() {
        AttendTransferDLG = null
        TransferDLG = null
    }

    private fun handleTransfer() {
        val currentLine = CallManager.Instance()!!.currentSession
        if (currentLine!!.state != CALL_STATE_FLAG.CONNECTED) {
            showTips("current line must be connected ")
            return
        }
        val transferTo = TransferDLG!!.findViewById<EditText>(R.id.ettransferto)
        val referTo = transferTo.text.toString()
        if (TextUtils.isEmpty(referTo)) {
            showTips("The transfer number is empty")
            return
        }
        val rt = application!!.mEngine!!.refer(currentLine.sessionID, referTo)
        if (rt != 0) {
            showTips(currentLine.lineName + ": failed to transfer")
        } else {
            showTips(currentLine.lineName + " success transfered")
        }
        TransferDLG = null
    }

    private fun handleAttendTransfer() {
        val currentLine = CallManager.Instance()!!.currentSession
        if (currentLine!!.state != CALL_STATE_FLAG.CONNECTED) {
            showTips("current line must be connected ")
            return
        }
        val transferTo = AttendTransferDLG!!.findViewById<EditText>(R.id.ettransferto)
        val transferLine = AttendTransferDLG!!.findViewById<EditText>(R.id.ettransferline)
        val referTo = transferTo.text.toString()
        if (TextUtils.isEmpty(referTo)) {
            showTips("The transfer number is empty")
            return
        }
        val lineString = transferLine.text.toString()
        val line = lineString.toInt()
        if (line < 0 || line >= CallManager.MAX_LINES) {
            showTips("The replace line out of range")
            return
        }
        val replaceSession = CallManager.Instance()!!.findSessionByIndex(line)
        if (replaceSession!!.state != CALL_STATE_FLAG.CONNECTED) {
            showTips("The replace line does not established yet")
            return
        }
        if (replaceSession.sessionID == currentLine.sessionID) {
            showTips("The replace line can not be current line")
            return
        }
        val rt = application!!.mEngine!!.attendedRefer(currentLine.sessionID, replaceSession.sessionID, referTo)
        if (rt != 0) {
            showTips(currentLine.lineName + ": failed to Attend transfer")
        } else {
            showTips(currentLine.lineName + ": Transferring")
        }
        AttendTransferDLG = null
    }

    override fun onClick(view: View) {
        if (application!!.mEngine == null) return
        val portSipSdk = application!!.mEngine
        val currentLine = CallManager.Instance()!!.currentSession
        when (view.id) {
            R.id.zero, R.id.one, R.id.two, R.id.three, R.id.four, R.id.five, R.id.six, R.id.seven, R.id.eight, R.id.nine, R.id.star, R.id.sharp -> {
                val numberString = (view as Button).text.toString()
                val number = numberString[0]
                etSipNum!!.append(numberString)
                if (CallManager.Instance()!!.regist && currentLine!!.state == CALL_STATE_FLAG.CONNECTED) {
                    if (number == '*') {
                        portSipSdk!!.sendDtmf(currentLine.sessionID, PortSipEnumDefine.ENUM_DTMF_MOTHOD_RFC2833, 10,
                                160, true)
                        return
                    }
                    if (number == '#') {
                        portSipSdk!!.sendDtmf(currentLine.sessionID, PortSipEnumDefine.ENUM_DTMF_MOTHOD_RFC2833, 11,
                                160, true)
                        return
                    }
                    val sum = numberString.toInt() // 0~9
                    portSipSdk!!.sendDtmf(currentLine.sessionID, PortSipEnumDefine.ENUM_DTMF_MOTHOD_RFC2833, sum,
                            160, true)
                }
            }
            R.id.delete -> {
                val cursorpos = etSipNum!!.selectionStart
                if (cursorpos - 1 >= 0) {
                    etSipNum!!.text.delete(cursorpos - 1, cursorpos)
                }
            }
            R.id.pad -> SwitchPanel()
            R.id.dial -> {
                val callTo = etSipNum!!.text.toString()
                if (callTo.length <= 0) {
                    showTips("The phone number is empty.")
                    return
                }
                if (!currentLine!!.IsIdle()) {
                    showTips("Current line is busy now, please switch a line.")
                    return
                }
                // Ensure that we have been added one audio codec at least
                if (portSipSdk!!.isAudioCodecEmpty) {
                    showTips("Audio Codec Empty,add audio codec at first")
                    return
                }
                // Usually for 3PCC need to make call without SDP
                val sessionId = portSipSdk.call(callTo, cbSendSdp!!.isChecked, cbSendVideo!!.isChecked)
                if (sessionId <= 0) {
                    showTips("Call failure")
                    return
                }
                //default send video
                portSipSdk.sendVideo(sessionId, true)
                currentLine.remote = callTo
                currentLine.sessionID = sessionId
                currentLine.state = CALL_STATE_FLAG.TRYING
                currentLine.hasVideo = cbSendVideo!!.isChecked
                showTips(currentLine.lineName + ": Calling...")
            }
            R.id.hangup -> {
                Ring.getInstance(getActivity())!!.stop()
                when (currentLine!!.state) {
                    CALL_STATE_FLAG.INCOMING -> {
                        portSipSdk!!.rejectCall(currentLine.sessionID, 486)
                        showTips(currentLine.lineName + ": Rejected call")
                    }
                    CALL_STATE_FLAG.CONNECTED, CALL_STATE_FLAG.TRYING -> {
                        portSipSdk!!.hangUp(currentLine.sessionID)
                        showTips(currentLine.lineName + ": Hang up")
                    }
                }
                currentLine.Reset()
            }
            R.id.answer -> {
                if (currentLine!!.state != CALL_STATE_FLAG.INCOMING) {
                    showTips("No incoming call on current line, please switch a line.")
                    return
                }
                currentLine.state = CALL_STATE_FLAG.CONNECTED
                Ring.getInstance(getActivity())!!.stopRingTone() //stop ring
                portSipSdk!!.answerCall(currentLine.sessionID, cbRecvVideo!!.isChecked) //answer call
                if (application!!.mConference) {
                    portSipSdk.joinToConference(currentLine.sessionID)
                }
            }
            R.id.reject -> {
                if (currentLine!!.state == CALL_STATE_FLAG.INCOMING) {
                    portSipSdk!!.rejectCall(currentLine.sessionID, 486)
                    currentLine.Reset()
                    Ring.getInstance(getActivity())!!.stop()
                    showTips(currentLine.lineName + ": Rejected call")
                    return
                }
            }
            R.id.hold -> {
                if (currentLine!!.state != CALL_STATE_FLAG.CONNECTED || currentLine.bHold) {
                    return
                }
                val rt = portSipSdk!!.hold(currentLine.sessionID)
                if (rt != 0) {
                    showTips("hold operation failed.")
                    return
                }
                currentLine.bHold = true
            }
            R.id.unhold -> {
                if (currentLine!!.state != CALL_STATE_FLAG.CONNECTED || !currentLine.bHold) {
                    return
                }
                val rt = portSipSdk!!.unHold(currentLine.sessionID)
                if (rt != 0) {
                    currentLine.bHold = false
                    showTips(currentLine.lineName + ": Un-Hold Failure.")
                    return
                }
                currentLine.bHold = false
                showTips(currentLine.lineName + ": Un-Hold")
            }
            R.id.attenttransfer -> {
                if (currentLine!!.state != CALL_STATE_FLAG.CONNECTED) {
                    showTips("Need to make the call established first")
                    return
                }
                showAttendTransferDialog()
            }
            R.id.transfer -> {
                if (currentLine!!.state != CALL_STATE_FLAG.CONNECTED) {
                    showTips("Need to make the call established first")
                    return
                }
                showTransferDialog()
            }
            R.id.mic -> if (CallManager.Instance()!!.setSpeakerOn(portSipSdk!!, !CallManager.Instance()!!.isSpeakerOn)) {
                (view as Button).text = "SpeakOn"
            } else {
                (view as Button).text = "SpeakOff"
            }
            R.id.mute -> {
                if (currentLine!!.bMute) {
                    portSipSdk!!.muteSession(currentLine.sessionID, false,
                            false, false, false)
                    currentLine!!.bMute = false
                    (view as Button).text = "Mute"
                } else {
                    portSipSdk!!.muteSession(currentLine.sessionID, true,
                            true, true, true)
                    currentLine!!.bMute = true
                    (view as Button).text = "UnMute"
                }
            }
        }
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view: View, position: Int, l: Long) {
        if (CallManager.Instance()!!.CurrentLine == position) {
            ShowCurrentLineState()
            return
        }
        if (cbConference!!.isChecked) {
            CallManager.Instance()!!.CurrentLine = position
            mtips!!.text = ""
        } else { // To switch the line, must hold currently line first
            var currentLine = CallManager.Instance()!!.currentSession
            if (currentLine!!.state == CALL_STATE_FLAG.CONNECTED && !currentLine.bHold) {
                application!!.mEngine!!.hold(currentLine.sessionID)
                currentLine.bHold = true
                showTips(currentLine.lineName + ": Hold")
            }
            CallManager.Instance()!!.CurrentLine = position
            currentLine = CallManager.Instance()!!.currentSession
            // If target line was in hold state, then un-hold it
            if (currentLine!!.IsIdle()) {
                showTips(currentLine.lineName + ": Idle")
            } else if (currentLine.state == CALL_STATE_FLAG.CONNECTED && currentLine.bHold) {
                application!!.mEngine!!.unHold(currentLine.sessionID)
                currentLine.bHold = false
                showTips(currentLine.lineName + ": UnHold - call established")
            }
        }
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}
    override fun onCheckedChanged(compoundButton: CompoundButton, b: Boolean) {
        when (compoundButton.id) {
            R.id.conference -> {
                if (application!!.mConference == b) return
                application!!.mConference = b
                if (b) {
                    application!!.mEngine!!.createVideoConference(null, 320, 240, true)
                    CallManager.Instance()!!.addActiveSessionToConfrence(application!!.mEngine!!)
                } else {
                    application!!.mEngine!!.destroyConference()
                }
            }
        }
    }
}