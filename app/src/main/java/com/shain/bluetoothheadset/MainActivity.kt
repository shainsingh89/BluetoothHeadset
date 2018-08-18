package com.shain.bluetoothheadset

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_audio_output.view.*

class MainActivity : AppCompatActivity(), BluetoothManager.BluetoothStateListener {

    private var mSoundPool: SoundPool? = null
    private var isBtHeadsetConnected: Boolean = false

    private lateinit var ringerManager: RingerManager
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.ringerManager = RingerManager(this)
        this.bluetoothManager = BluetoothManager(this, this)

        buttonPlayPause.setOnClickListener {
            if (buttonPlayPause.tag == PAUSE) {
                bluetoothManager.startBluetoothConnection(false)
                ringerManager.stop()
                buttonPlayPause.tag = PLAY
                buttonPlayPause.text = PLAY

            } else {
                ringerManager.startOutgoingRinger(OutgoingRinger.Type.SONAR)
                bluetoothManager.startBluetoothConnection(true)
                buttonPlayPause.tag = PAUSE
                buttonPlayPause.text = PAUSE
                imgOutputDevice.setImageResource(R.drawable.ic_phone_black_24dp)
            }
        }

        buttonSwitch.setOnClickListener {
            bottomSheetDialog.show()
        }
        audioManager = ServiceUtil.getAudioManager(this)
        initBottomSheet()
    }

    private fun initBottomSheet() {

        val modelBottomSheet = layoutInflater.inflate(R.layout.layout_audio_output, null)

        modelBottomSheet.tv_btn_bluetooth.setOnClickListener { onAudioOutputOptionClicked(it as TextView) }
        modelBottomSheet.tv_btn_speaker.setOnClickListener { onAudioOutputOptionClicked(it as TextView) }
        modelBottomSheet.tv_btn_earphone.setOnClickListener { onAudioOutputOptionClicked(it as TextView) }

        bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(modelBottomSheet)
    }

    private fun onAudioOutputOptionClicked(view: TextView) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (view.id) {
            R.id.tv_btn_bluetooth -> {
                if (isBtHeadsetConnected) {
                    imgOutputDevice.setImageResource(R.drawable.ic_bluetooth_audio_black_24dp)
                    audioManager.isSpeakerphoneOn = false
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                } else {
                    toast("Bluetooth device not connected")
                }
            }
            R.id.tv_btn_speaker -> {
                imgOutputDevice.setImageResource(R.drawable.ic_volume_up_black_24dp)
                if (audioManager.isBluetoothScoOn) {
                    audioManager.stopBluetoothSco()
                    audioManager.isBluetoothScoOn = false
                }
                audioManager.isSpeakerphoneOn = true
            }
            R.id.tv_btn_earphone -> {
                imgOutputDevice.setImageResource(R.drawable.ic_phone_black_24dp)
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                audioManager.isSpeakerphoneOn = false
            }
        }
        bottomSheetDialog.dismiss()
        onAudioSettingsChanged()
    }

    override fun onStop() {
        super.onStop()
        if (mSoundPool != null) mSoundPool!!.release()
        bluetoothManager.onDestroy()
    }

    private fun onAudioSettingsChanged() {
        if (!isBtHeadsetConnected) {
            Log.e(TAG, "onAudioSettingsChanged: Nothing connected")
        } else {
            if (isBtHeadsetConnected) {
                when {
                    audioManager.isBluetoothScoOn -> {
                        imgOutputDevice.setImageResource(R.drawable.ic_bluetooth_audio_black_24dp)
                        Log.d(TAG, "onAudioSettingsChanged: Bluetooth " + audioManager.isBluetoothScoOn)
                    }
                    audioManager.isSpeakerphoneOn -> {
                        imgOutputDevice.setImageResource(R.drawable.ic_volume_up_black_24dp)
                        Log.d(TAG, "onAudioSettingsChanged: Speaker ")
                    }
                    else -> {
                        imgOutputDevice.setImageResource(R.drawable.ic_phone_black_24dp)
                        Log.d(TAG, "onAudioSettingsChanged: phone ")
                    }
                }
            } else {
                Log.d(TAG, "onAudioSettingsChanged: Speaker " + audioManager.isSpeakerphoneOn)
            }
        }
    }

    override fun onBluetoothStateChanged(isAvailable: Boolean) {
        onAudioSettingsChanged()
        isBtHeadsetConnected = isAvailable
    }

    private fun Context.toast(message: CharSequence) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "MainActivity"

        private const val PLAY = "PLAY"
        private const val PAUSE = "PAUSE"
    }
}
