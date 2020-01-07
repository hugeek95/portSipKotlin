package com.portsip.sipsample.ui

import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.view.View
import com.portsip.R
import com.portsip.sipsample.service.PortSipService.Companion.ConfigPreferences

class SettingFragment : PreferenceFragment() {
    var application: MyApplication? = null
    var activity: MainActivity? = null
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        activity = getActivity() as MainActivity
        application = activity!!.application as MyApplication
        addPreferencesFromResource(R.xml.setting)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(resources.getColor(R.color.white))
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(getActivity())
            ConfigPreferences(getActivity(), preferences, application!!.mEngine)
        } else {
            activity!!.receiver!!.broadcastReceiver = null
        }
    }
}