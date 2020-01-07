package com.portsip.sipsample.ui

import android.app.Fragment
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener

open class BaseFragment : Fragment(), OnTouchListener {
    override fun onViewCreated(view: View, savedInstanceState: Bundle) {
        view.setOnTouchListener(this)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return true
    }
}