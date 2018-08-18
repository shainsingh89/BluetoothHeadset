package com.shain.bluetoothheadset

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

import java.io.IOException

class OutgoingRinger(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    enum class Type {
        SONAR,
        RINGING,
        BUSY
    }

    fun start(type: Type) {
        val soundId: Int

        if (type == Type.SONAR)
            soundId = R.raw.voip_ringback
        else
            throw IllegalArgumentException("Not a valid sound type")

        if (mediaPlayer != null) {
            mediaPlayer!!.release()
        }

        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
        mediaPlayer!!.isLooping = true

        val packageName = context.packageName
        val dataUri = Uri.parse("android.resource://$packageName/$soundId")

        try {
            mediaPlayer!!.setDataSource(context, dataUri)
            mediaPlayer!!.prepare()
            mediaPlayer!!.start()
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, e)
        } catch (e: SecurityException) {
            Log.w(TAG, e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, e)
        } catch (e: IOException) {
            Log.w(TAG, e)
        }

    }

    fun stop() {
        if (mediaPlayer == null) return
        mediaPlayer!!.release()
        mediaPlayer = null
    }

    companion object {

        private val TAG = OutgoingRinger::class.java.simpleName
    }
}
