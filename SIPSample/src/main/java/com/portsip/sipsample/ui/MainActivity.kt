package com.portsip.sipsample.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.core.app.ActivityCompat
import com.portsip.R
import com.portsip.sipsample.receiver.PortMessageReceiver
import com.portsip.sipsample.service.PortSipService

class MainActivity : Activity(), RadioGroup.OnCheckedChangeListener {
    @JvmField
    var receiver: PortMessageReceiver? = null
    private val REQ_DANGERS_PERMISSION = 2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiver = PortMessageReceiver()
        setContentView(R.layout.main)
        val filter = IntentFilter()
        filter.addAction(PortSipService.REGISTER_CHANGE_ACTION)
        filter.addAction(PortSipService.CALL_CHANGE_ACTION)
        filter.addAction(PortSipService.PRESENCE_CHANGE_ACTION)
        registerReceiver(receiver, filter)
        switchContent(R.id.login_fragment)
        val menuGroup = findViewById<RadioGroup>(R.id.tab_menu)
        menuGroup.setOnCheckedChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        requestPermissions(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQ_DANGERS_PERMISSION -> {
                var i = 0
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "you must grant the permission " + permissions[i], Toast.LENGTH_SHORT).show()
                        i++
                        stopService(Intent(this, PortSipService::class.java))
                        System.exit(0)
                    }
                }
            }
        }
    }

    override fun onCheckedChanged(radioGroup: RadioGroup, checkedId: Int) {
        when (checkedId) {
            R.id.tab_login -> switchContent(R.id.login_fragment)
            R.id.tab_numpad -> switchContent(R.id.numpad_fragment)
            R.id.tab_video -> switchContent(R.id.video_fragment)
            R.id.tab_message -> switchContent(R.id.message_fragment)
            R.id.tab_setting -> switchContent(R.id.setting_fragment)
        }
    }

    private fun switchContent(@IdRes fragmentId: Int) {
        val fragment = fragmentManager.findFragmentById(fragmentId)
        val login_fragment = fragmentManager.findFragmentById(R.id.login_fragment)
        val numpad_fragment = fragmentManager.findFragmentById(R.id.numpad_fragment)
        val video_fragment = fragmentManager.findFragmentById(R.id.video_fragment)
        val setting_fragment = fragmentManager.findFragmentById(R.id.setting_fragment)
        val message_fragment = fragmentManager.findFragmentById(R.id.message_fragment)
        val fTransaction = fragmentManager.beginTransaction()
        fTransaction.hide(login_fragment).hide(numpad_fragment).hide(video_fragment).hide(setting_fragment).hide(message_fragment)
        if (fragment != null) {
            fTransaction.show(fragment).commit()
        }
    }

    fun requestPermissions(activity: Activity?) { // Check if we have write permission
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) || PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) || PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                    REQ_DANGERS_PERMISSION)
        }
    }
}