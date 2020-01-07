package com.portsip.sipsample.util

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.provider.Settings

class Ring private constructor(context: Context) {
    private var mRingbackPlayer: ToneGenerator? = null
    protected var mRingtonePlayer: Ringtone? = null
    var ringRef = 0
    private val mContext: Context?
    private var savedMode = AudioManager.MODE_INVALID
    var audioManager: AudioManager
    fun stop(): Boolean {
        stopRingBackTone()
        stopRingTone()
        return true
    }

    fun startRingTone() {
        if (mRingtonePlayer != null && mRingtonePlayer!!.isPlaying) {
            ringRef++
            return
        }
        if (mRingtonePlayer == null && mContext != null) {
            mRingtonePlayer = RingtoneManager.getRingtone(mContext, Settings.System.DEFAULT_RINGTONE_URI)
        }
        savedMode = audioManager.mode
        audioManager.mode = AudioManager.MODE_RINGTONE
        if (mRingtonePlayer != null) {
            synchronized(mRingtonePlayer!!) {
                ringRef++
                mRingtonePlayer!!.play()
            }
        }
    }

    fun stopRingTone() {
        if (mRingtonePlayer != null) {
            synchronized(mRingtonePlayer!!) {
                if (--ringRef <= 0) {
                    audioManager.mode = savedMode
                    mRingtonePlayer!!.stop()
                    mRingtonePlayer = null
                }
            }
        }
    }

    fun startRingBackTone() {
        if (mRingbackPlayer == null) {
            mRingbackPlayer = try {
                ToneGenerator(AudioManager.STREAM_VOICE_CALL, TONE_RELATIVE_VOLUME)
            } catch (e: RuntimeException) {
                null
            }
        }
        if (mRingbackPlayer != null) {
            synchronized(mRingbackPlayer!!) { mRingbackPlayer!!.startTone(ToneGenerator.TONE_SUP_RINGTONE) }
        }
    }

    fun stopRingBackTone() {
        if (mRingbackPlayer != null) {
            synchronized(mRingbackPlayer!!) {
                mRingbackPlayer!!.stopTone()
                mRingbackPlayer = null
            }
        }
    }

    companion object {
        private const val TONE_RELATIVE_VOLUME = 70
        private var single: Ring? = null
        fun getInstance(context: Context): Ring? {
            if (single == null) {
                single = Ring(context)
            }
            return single
        }
    }

    init {
        mContext = context
        audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}