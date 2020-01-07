package com.portsip.sipsample.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import com.portsip.PortSipSdk
import com.portsip.R
import com.portsip.sipsample.adapter.ContactAdapter
import com.portsip.sipsample.receiver.PortMessageReceiver.BroadcastListener
import com.portsip.sipsample.service.PortSipService
import com.portsip.sipsample.util.CallManager
import com.portsip.sipsample.util.Contact
import com.portsip.sipsample.util.ContactManager

class MessageFragment : BaseFragment(), View.OnClickListener, BroadcastListener {
    var etContact: EditText? = null
    var etStatus: EditText? = null
    var etmsgdest: EditText? = null
    var etMessage: EditText? = null
    var lvContacts: ListView? = null
    var application: MyApplication? = null
    var activity: MainActivity? = null
    private var mAdapter: ContactAdapter? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        activity = getActivity() as MainActivity
        application = activity!!.application as MyApplication
        return inflater.inflate(R.layout.message, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState!!)
        lvContacts = view.findViewById<View>(R.id.lvcontacs) as ListView
        view.findViewById<View>(R.id.btsubscribe).setOnClickListener(this)
        view.findViewById<View>(R.id.btclear).setOnClickListener(this)
        mAdapter = ContactAdapter(getActivity(), ContactManager.Instance()!!.contacts)
        lvContacts!!.adapter = mAdapter
        etStatus = view.findViewById<View>(R.id.etstatus) as EditText
        etMessage = view.findViewById<View>(R.id.etmessage) as EditText
        etContact = view.findViewById<View>(R.id.etcontact) as EditText
        etmsgdest = view.findViewById<View>(R.id.etmsgdest) as EditText
        view.findViewById<View>(R.id.btsendmsg).setOnClickListener(this)
        view.findViewById<View>(R.id.btsendstatus).setOnClickListener(this)
        view.findViewById<View>(R.id.btaddcontact).setOnClickListener(this)
        view.findViewById<View>(R.id.btaccept).setOnClickListener(this)
        view.findViewById<View>(R.id.btrefuse).setOnClickListener(this)
        view.findViewById<View>(R.id.btsubscribe).setOnClickListener(this)
        onHiddenChanged(false)
    }

    private fun btnAddContact_Click(sdk: PortSipSdk) {
        if (!isOnline) return
        val sendTo = etContact!!.text.toString()
        if (TextUtils.isEmpty(sendTo)) {
            return
        }
        var contact = ContactManager.Instance()!!.findContactBySipAddr(sendTo)
        if (contact == null) {
            contact = Contact()
            contact.sipAddr = sendTo
            ContactManager.Instance()!!.addContact(contact)
        }
        updateLV()
    }

    private fun btnSubscribeContact_Click(sdk: PortSipSdk) {
        if (!isOnline) return
        val contact = selectContact
        if (contact != null) {
            sdk.presenceSubscribe(contact.sipAddr, "hello") //subscribe remote
            contact.subScribRemote = true
        }
        updateLV()
    }

    private fun btnClearContact_Click() {
        ContactManager.Instance()!!.removeAll()
        updateLV()
    }

    private fun updateLV() {
        mAdapter!!.notifyDataSetChanged()
    }

    private fun btnSetStatus_Click(sdk: PortSipSdk) {
        if (!isOnline) return
        val content = etStatus!!.text.toString()
        if (TextUtils.isEmpty(content)) { //showTips("please input status description string");
            return
        }
        val contacts = ContactManager.Instance()!!.contacts
        for (contact in contacts) {
            val subscribeId = contact.subId
            val statusText = etStatus!!.text.toString()
            if (contact.state == Contact.SUBSCRIBE_STATE_FLAG.ACCEPTED) //向已经接受的订阅，发布自己的出席状态
            {
                sdk.setPresenceStatus(subscribeId, statusText)
            }
        }
    }

    private fun btnAcceptSubscribe_Click(sdk: PortSipSdk) {
        if (!isOnline) return
        val contact = selectContact
        if (contact != null && contact.state == Contact.SUBSCRIBE_STATE_FLAG.UNSETTLLED) {
            sdk.presenceAcceptSubscribe(contact.subId) //accept
            contact.state = Contact.SUBSCRIBE_STATE_FLAG.ACCEPTED
            var status = etStatus!!.text.toString()
            if (!TextUtils.isEmpty(status)) {
                status = "hello"
            }
            sdk.setPresenceStatus(contact.subId, status) //set my status
        }
        updateLV()
    }

    private fun btnRefuseSubscribe_Click(sdk: PortSipSdk) {
        if (!isOnline) return
        val contact = selectContact
        if (contact != null && contact.state == Contact.SUBSCRIBE_STATE_FLAG.UNSETTLLED) {
            sdk.presenceRejectSubscribe(contact.subId) //reject
            contact.state = Contact.SUBSCRIBE_STATE_FLAG.REJECTED // reject subscribe
            contact.subId = 0
        }
        updateLV()
    }

    private fun btnSend_Click(sdk: PortSipSdk) {
        if (!isOnline) return
        val content = etMessage!!.text.toString()
        val sendTo = etmsgdest!!.text.toString()
        if (TextUtils.isEmpty(sendTo)) {
            Toast.makeText(getActivity(), "Please input send to target",
                    Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(getActivity(), "Please input message content",
                    Toast.LENGTH_SHORT).show()
            return
        }
        val contentBinary = content.toByteArray()
        if (contentBinary != null) {
            sdk.sendOutOfDialogMessage(sendTo, "text", "plain", false,
                    contentBinary, contentBinary.size)
        }
    }

    override fun onBroadcastReceiver(intent: Intent?) {
        val action = if (intent == null) "" else intent.action
        if (PortSipService.PRESENCE_CHANGE_ACTION == action) {
            updateLV()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            activity!!.receiver!!.broadcastReceiver = this
            updateLV()
        }
    }

    private val isOnline: Boolean
        private get() {
            if (!CallManager.Instance()!!.regist) {
                Toast.makeText(getActivity(), "Please login at first", Toast.LENGTH_SHORT).show()
            }
            return CallManager.Instance()!!.regist
        }

    private val selectContact: Contact?
        private get() {
            val contacts = ContactManager.Instance()!!.contacts
            val checkedItemPosition = lvContacts!!.checkedItemPosition
            return if (ListView.INVALID_POSITION != checkedItemPosition && contacts.size > checkedItemPosition) {
                contacts[checkedItemPosition]
            } else null
        }

    override fun onClick(view: View) {
        if (application!!.mEngine == null) {
            return
        }
        when (view.id) {
            R.id.btsendmsg -> btnSend_Click(application!!.mEngine!!)
            R.id.btsendstatus -> btnSetStatus_Click(application!!.mEngine!!)
            R.id.btaddcontact -> btnAddContact_Click(application!!.mEngine!!)
            R.id.btclear -> btnClearContact_Click()
            R.id.btsubscribe -> btnSubscribeContact_Click(application!!.mEngine!!)
            R.id.btaccept -> btnAcceptSubscribe_Click(application!!.mEngine!!)
            R.id.btrefuse -> btnRefuseSubscribe_Click(application!!.mEngine!!)
            else -> {
            }
        }
    }
}