package com.shain.bluetoothheadset

import android.content.Context
import android.media.AudioManager

object ServiceUtil {

    fun getAudioManager(context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
}
