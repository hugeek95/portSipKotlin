package com.portsip.sipsample.ui

import android.app.Application
import com.portsip.PortSipSdk

class MyApplication : Application() {
    @JvmField
	var mConference = false
    @JvmField
	var mEngine: PortSipSdk? = null
    @JvmField
	var mUseFrontCamera = false
    override fun onCreate() {
        super.onCreate()
        mEngine = PortSipSdk()
    }
}