package com.portsip.sipsample.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.portsip.PortSIPVideoRenderer
import com.portsip.PortSipErrorcode
import com.portsip.PortSipSdk
import com.portsip.R
import com.portsip.sipsample.receiver.PortMessageReceiver.BroadcastListener
import com.portsip.sipsample.service.PortSipService
import com.portsip.sipsample.util.CallManager
import com.portsip.sipsample.util.Session
import com.portsip.sipsample.util.Session.CALL_STATE_FLAG

class VideoFragment : BaseFragment(), View.OnClickListener, BroadcastListener {
    var application: MyApplication? = null
    var activity: MainActivity? = null
    private var remoteRenderScreen: PortSIPVideoRenderer? = null
    private var localRenderScreen: PortSIPVideoRenderer? = null
    private var scalingType = PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_BALANCED // SCALE_ASPECT_FIT or SCALE_ASPECT_FILL;
    private var imgSwitchCamera: ImageButton? = null
    private var imgScaleType: ImageButton? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        activity = getActivity() as MainActivity
        application = activity!!.application as MyApplication
        return inflater.inflate(R.layout.video, container, false)
    }

    fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState!!)
        imgSwitchCamera = view.findViewById<View>(R.id.ibcamera) as ImageButton
        imgScaleType = view.findViewById<View>(R.id.ibscale) as ImageButton
        imgScaleType!!.setOnClickListener(this)
        imgSwitchCamera!!.setOnClickListener(this)
        localRenderScreen = view.findViewById<View>(R.id.local_video_view) as PortSIPVideoRenderer
        remoteRenderScreen = view.findViewById<View>(R.id.remote_video_view) as PortSIPVideoRenderer
        scalingType = PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FIT //
        remoteRenderScreen!!.setScalingType(scalingType)
        activity!!.receiver!!.broadcastReceiver = this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val portSipLib = application!!.mEngine
        if (localRenderScreen != null) {
            portSipLib?.displayLocalVideo(false)
            localRenderScreen!!.release()
        }
        CallManager.Instance()!!.setRemoteVideoWindow(application!!.mEngine!!, -1, null) //set
        if (remoteRenderScreen != null) {
            remoteRenderScreen!!.release()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            localRenderScreen!!.visibility = View.INVISIBLE
            stopVideo(application!!.mEngine)
        } else {
            updateVideo(application!!.mEngine)
            activity!!.receiver!!.broadcastReceiver = this
            localRenderScreen!!.visibility = View.VISIBLE
        }
    }

    override fun onClick(v: View) {
        val portSipLib = application!!.mEngine
        when (v.id) {
            R.id.ibcamera -> {
                application!!.mUseFrontCamera = !application!!.mUseFrontCamera
                SetCamera(portSipLib, application!!.mUseFrontCamera)
            }
            R.id.ibscale -> {
                scalingType = if (scalingType == PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FIT) {
                    imgScaleType!!.setImageResource(R.drawable.aspect_fill)
                    PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FILL
                } else if (scalingType == PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FILL) {
                    imgScaleType!!.setImageResource(R.drawable.aspect_balanced)
                    PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_BALANCED
                } else {
                    imgScaleType!!.setImageResource(R.drawable.aspect_fit)
                    PortSIPVideoRenderer.ScalingType.SCALE_ASPECT_FIT
                }
                localRenderScreen!!.setScalingType(scalingType)
                remoteRenderScreen!!.setScalingType(scalingType)
                updateVideo(portSipLib)
            }
        }
    }

    private fun SetCamera(portSipLib: PortSipSdk?, userFront: Boolean) {
        if (userFront) {
            portSipLib!!.setVideoDeviceId(1)
        } else {
            portSipLib!!.setVideoDeviceId(0)
        }
    }

    private fun stopVideo(portSipLib: PortSipSdk?) {
        val cur = CallManager.Instance()!!.currentSession
        if (portSipLib != null) {
            portSipLib.displayLocalVideo(false)
            portSipLib.setLocalVideoWindow(null)
            CallManager.Instance()!!.setRemoteVideoWindow(portSipLib, cur!!.sessionID, null)
            CallManager.Instance()!!.setConferenceVideoWindow(portSipLib, null)
        }
    }

    fun updateVideo(portSipLib: PortSipSdk?) {
        val callManager = CallManager.Instance()
        if (application!!.mConference) {
            callManager!!.setConferenceVideoWindow(portSipLib!!, remoteRenderScreen)
        } else {
            val cur = CallManager.Instance()!!.currentSession
            if (cur != null && !cur.IsIdle()
                    && cur.sessionID != PortSipErrorcode.INVALID_SESSION_ID.toLong() && cur.hasVideo) {
                callManager!!.setRemoteVideoWindow(portSipLib!!, cur.sessionID, remoteRenderScreen)
                portSipLib!!.setLocalVideoWindow(localRenderScreen)
                portSipLib.displayLocalVideo(true) // display Local video
                portSipLib.sendVideo(cur.sessionID, true)
            } else {
                portSipLib!!.displayLocalVideo(false)
                callManager!!.setRemoteVideoWindow(portSipLib, cur!!.sessionID, null)
                portSipLib.setLocalVideoWindow(null)
            }
        }
    }

    override fun onBroadcastReceiver(intent: Intent?) {
        val action = if (intent == null) "" else intent.action
        if (PortSipService.CALL_CHANGE_ACTION == action) {
            val sessionId = intent!!.getLongExtra(PortSipService.EXTRA_CALL_SEESIONID, Session.INVALID_SESSION_ID.toLong())
            val status = intent.getStringExtra(PortSipService.EXTRA_CALL_DESCRIPTION)
            val session = CallManager.Instance()!!.findSessionBySessionID(sessionId)
            if (session != null) {
                when (session.state) {
                    CALL_STATE_FLAG.INCOMING -> {
                    }
                    CALL_STATE_FLAG.TRYING -> {
                    }
                    CALL_STATE_FLAG.CONNECTED, CALL_STATE_FLAG.FAILED, CALL_STATE_FLAG.CLOSED -> updateVideo(application!!.mEngine)
                }
            }
        }
    }
}