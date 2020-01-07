package com.portsip.sipsample.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import com.portsip.R
import com.portsip.sipsample.receiver.PortMessageReceiver.BroadcastListener
import com.portsip.sipsample.service.PortSipService
import com.portsip.sipsample.util.CallManager

class LoginFragment : BaseFragment(), OnItemSelectedListener, View.OnClickListener, BroadcastListener {
    var application: MyApplication? = null
    var activity: MainActivity? = null
    private var etUsername: EditText? = null
    private var etPassword: EditText? = null
    private var etSipServer: EditText? = null
    private var etSipServerPort: EditText? = null
    private var etDisplayname: EditText? = null
    private var etUserdomain: EditText? = null
    private var etAuthName: EditText? = null
    private var etStunServer: EditText? = null
    private var etStunPort: EditText? = null
    private var spTransport: Spinner? = null
    private var spSRTP: Spinner? = null
    private var mtxStatus: TextView? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = getActivity() as MainActivity
        application = activity!!.applicationContext as MyApplication
        return inflater.inflate(R.layout.login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle) {
        super.onViewCreated(view, savedInstanceState)
        mtxStatus = view.findViewById<View>(R.id.txtips) as TextView
        etUsername = view.findViewById<View>(R.id.etusername) as EditText
        etPassword = view.findViewById<View>(R.id.etpwd) as EditText
        etSipServer = view.findViewById<View>(R.id.etsipsrv) as EditText
        etSipServerPort = view.findViewById<View>(R.id.etsipport) as EditText
        etDisplayname = view.findViewById<View>(R.id.etdisplayname) as EditText
        etUserdomain = view.findViewById<View>(R.id.etuserdomain) as EditText
        etAuthName = view.findViewById<View>(R.id.etauthName) as EditText
        etStunServer = view.findViewById<View>(R.id.etStunServer) as EditText
        etStunPort = view.findViewById<View>(R.id.etStunPort) as EditText
        spTransport = view.findViewById<View>(R.id.spTransport) as Spinner
        spSRTP = view.findViewById<View>(R.id.spSRTP) as Spinner
        spTransport!!.adapter = ArrayAdapter.createFromResource(getActivity(), R.array.transports, android.R.layout.simple_list_item_1)
        spSRTP!!.adapter = ArrayAdapter.createFromResource(getActivity(), R.array.srtp, android.R.layout.simple_list_item_1)
        spSRTP!!.onItemSelectedListener = this
        spTransport!!.onItemSelectedListener = this
        LoadUserInfo()
        setOnlineStatus(null)
        activity!!.receiver!!.broadcastReceiver = this
        view.findViewById<View>(R.id.btonline).setOnClickListener(this)
        view.findViewById<View>(R.id.btoffline).setOnClickListener(this)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            activity!!.receiver!!.broadcastReceiver = this
            setOnlineStatus(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity!!.receiver!!.broadcastReceiver = null
    }

    private fun LoadUserInfo() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(getActivity())
        etUsername!!.setText(preferences.getString(PortSipService.USER_NAME, null))
        etPassword!!.setText(preferences.getString(PortSipService.USER_PWD, null))
        etSipServer!!.setText(preferences.getString(PortSipService.SVR_HOST, null))
        etSipServerPort!!.setText(preferences.getString(PortSipService.SVR_PORT, "5060"))
        etDisplayname!!.setText(preferences.getString(PortSipService.USER_DISPALYNAME, null))
        etUserdomain!!.setText(preferences.getString(PortSipService.USER_DOMAIN, null))
        etAuthName!!.setText(preferences.getString(PortSipService.USER_AUTHNAME, null))
        etStunServer!!.setText(preferences.getString(PortSipService.STUN_HOST, null))
        etStunPort!!.setText(preferences.getString(PortSipService.STUN_PORT, "3478"))
        spTransport!!.setSelection(preferences.getInt(PortSipService.TRANS, 0))
        spSRTP!!.setSelection(preferences.getInt(PortSipService.SRTP, 0))
    }

    private fun SaveUserInfo() {
        val editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
        editor.putString(PortSipService.USER_NAME, etUsername!!.text.toString())
        editor.putString(PortSipService.USER_PWD, etPassword!!.text.toString())
        editor.putString(PortSipService.SVR_HOST, etSipServer!!.text.toString())
        editor.putString(PortSipService.SVR_PORT, etSipServerPort!!.text.toString())
        editor.putString(PortSipService.USER_DISPALYNAME, etDisplayname!!.text.toString())
        editor.putString(PortSipService.USER_DOMAIN, etUserdomain!!.text.toString())
        editor.putString(PortSipService.USER_AUTHNAME, etAuthName!!.text.toString())
        editor.putString(PortSipService.STUN_HOST, etStunServer!!.text.toString())
        editor.putString(PortSipService.STUN_PORT, etStunPort!!.text.toString())
        editor.commit()
    }

    override fun onBroadcastReceiver(intent: Intent?) {
        val action = if (intent == null) "" else intent.action
        if (PortSipService.REGISTER_CHANGE_ACTION == action) {
            val tips = intent!!.getStringExtra(PortSipService.EXTRA_REGISTER_STATE)
            setOnlineStatus(tips)
        } else if (PortSipService.CALL_CHANGE_ACTION == action) { //long sessionId = intent.GetLongExtra(PortSipService.EXTRA_CALL_SEESIONID, Session.INVALID_SESSION_ID);
//callStatusChanged(sessionId);
        }
    }

    private fun setOnlineStatus(tips: String?) {
        if (CallManager.Instance()!!.regist) {
            mtxStatus!!.text = if (TextUtils.isEmpty(tips)) getString(R.string.online) else tips
        } else {
            mtxStatus!!.text = if (TextUtils.isEmpty(tips)) getString(R.string.offline) else tips
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btonline -> {
                SaveUserInfo()
                val onLineIntent = Intent(getActivity(), PortSipService::class.java)
                onLineIntent.action = PortSipService.ACTION_SIP_REGIEST
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getActivity().startForegroundService(onLineIntent)
                } else {
                    getActivity().startService(onLineIntent)
                }
                mtxStatus!!.text = "RegisterServer.."
            }
            R.id.btoffline -> {
                val offLineIntent = Intent(getActivity(), PortSipService::class.java)
                offLineIntent.action = PortSipService.ACTION_SIP_UNREGIEST
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getActivity().startForegroundService(offLineIntent)
                } else {
                    getActivity().startService(offLineIntent)
                }
                mtxStatus!!.text = "unRegisterServer"
            }
        }
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view: View, position: Int, l: Long) {
        if (adapterView == null) return
        val editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
        when (adapterView.id) {
            R.id.spSRTP -> editor.putInt(PortSipService.SRTP, position).commit()
            R.id.spTransport -> editor.putInt(PortSipService.TRANS, position).commit()
        }
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}
}