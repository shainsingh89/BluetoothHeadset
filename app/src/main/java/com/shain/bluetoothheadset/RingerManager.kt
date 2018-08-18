package com.shain.bluetoothheadset


import android.content.Context
import android.media.AudioManager


class RingerManager(context: Context) {

    private val context: Context = context.applicationContext
    private val outgoingRinger: OutgoingRinger = OutgoingRinger(context)

    fun startOutgoingRinger(type: OutgoingRinger.Type) {
        val audioManager = ServiceUtil.getAudioManager(context)
        audioManager.isMicrophoneMute = false

        if (type == OutgoingRinger.Type.SONAR) {
            audioManager.isSpeakerphoneOn = false
        }

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        outgoingRinger.stop()
        outgoingRinger.start(type)
    }

    fun stop() {

        val audioManager = ServiceUtil.getAudioManager(context)
        outgoingRinger.stop()

        if (audioManager.isBluetoothScoOn) {
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()
        }

        audioManager.isSpeakerphoneOn = false
        audioManager.isMicrophoneMute = false
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.abandonAudioFocus(null)
    }

    companion object {

        private val TAG = RingerManager::class.java.simpleName
    }
}
