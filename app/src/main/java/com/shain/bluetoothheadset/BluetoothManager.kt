package com.shain.bluetoothheadset

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log

class BluetoothManager internal constructor(context: Context, private val listener: BluetoothStateListener?) {

    private val ObjectLock = Any()

    private val context: Context = context.applicationContext
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothScoReceiver: BluetoothScoReceiver? = null
    private var bluetoothConnectionReceiver: BluetoothConnectionReceiver? = null

    private var bluetoothHeadset: BluetoothHeadset? = null
    private var scoConnection = ScoConnection.DISCONNECTED
    private var wantsConnection = false

    private val isBluetoothAvailable: Boolean
        get() {
            try {
                synchronized(ObjectLock) {
                    val audioManager = ServiceUtil.getAudioManager(context)

                    if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return false
                    return if (!audioManager.isBluetoothScoAvailableOffCall) false else bluetoothHeadset != null && !bluetoothHeadset!!.connectedDevices.isEmpty()
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                return false
            }

        }

    private val scoChangeIntent: String
        get() = AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED

    private enum class ScoConnection {
        DISCONNECTED,
        IN_PROGRESS,
        CONNECTED
    }

    init {
        this.bluetoothScoReceiver = BluetoothScoReceiver()
        this.bluetoothConnectionReceiver = BluetoothConnectionReceiver()

        if (this.bluetoothAdapter != null) {
            requestHeadsetProxyProfile()

            this.context.registerReceiver(bluetoothConnectionReceiver, IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED))

            val sticky = this.context.registerReceiver(bluetoothScoReceiver, IntentFilter(scoChangeIntent))

            if (sticky != null) {
                bluetoothScoReceiver!!.onReceive(context, sticky)
            }

            handleBluetoothStateChange()
        }
    }

    fun onDestroy() {
        if (bluetoothHeadset != null && bluetoothAdapter != null) {
            this.bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
        }

        if (bluetoothConnectionReceiver != null) {
            context.unregisterReceiver(bluetoothConnectionReceiver)
            bluetoothConnectionReceiver = null
        }

        if (bluetoothScoReceiver != null) {
            context.unregisterReceiver(bluetoothScoReceiver)
            bluetoothScoReceiver = null
        }

        this.bluetoothHeadset = null
    }

    fun startBluetoothConnection(enabled: Boolean) {
        synchronized(ObjectLock) {
            val audioManager = ServiceUtil.getAudioManager(context)

            this.wantsConnection = enabled

            if (wantsConnection && isBluetoothAvailable && scoConnection == ScoConnection.DISCONNECTED) {
                audioManager.startBluetoothSco()
                scoConnection = ScoConnection.IN_PROGRESS
            } else if (!wantsConnection && scoConnection == ScoConnection.CONNECTED) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
                scoConnection = ScoConnection.DISCONNECTED
            } else if (!wantsConnection && scoConnection == ScoConnection.IN_PROGRESS) {
                audioManager.stopBluetoothSco()
                scoConnection = ScoConnection.DISCONNECTED
            }
        }
    }

    private fun handleBluetoothStateChange() {
        listener?.onBluetoothStateChanged(isBluetoothAvailable)
    }


    private fun requestHeadsetProxyProfile() {
        this.bluetoothAdapter!!.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) {
                    synchronized(ObjectLock) {
                        bluetoothHeadset = proxy as BluetoothHeadset
                    }

                    val b = proxy.connectedDevices
                    val stringBuilder = StringBuilder()
                    for (getConnectedDevice in b) {
                        Log.d(TAG, "onServiceConnected: " + getConnectedDevice.name)
                        stringBuilder.append(getConnectedDevice.name)
                    }

                    synchronized(ObjectLock) {
                        if (wantsConnection && isBluetoothAvailable && scoConnection == ScoConnection.DISCONNECTED) {
                            val audioManager = ServiceUtil.getAudioManager(context)
                            audioManager.startBluetoothSco()
                            scoConnection = ScoConnection.IN_PROGRESS
                        }
                    }

                    handleBluetoothStateChange()
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                Log.d(TAG, "onServiceDisconnected")
                if (profile == BluetoothProfile.HEADSET) {
                    bluetoothHeadset = null
                    handleBluetoothStateChange()
                }
            }
        }, BluetoothProfile.HEADSET)
    }

    private inner class BluetoothScoReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null || bluetoothHeadset == null) return
            Log.w(TAG, "onReceive")

            synchronized(ObjectLock) {
                if (scoChangeIntent == intent.action) {
                    val status = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR)

                    if (status == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                        val devices = bluetoothHeadset!!.connectedDevices

                        for (device in devices) {
                            if (bluetoothHeadset!!.isAudioConnected(device)) {
                                val deviceClass = device.bluetoothClass.deviceClass

                                if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||
                                        deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                                        deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
                                    scoConnection = ScoConnection.CONNECTED

                                    if (wantsConnection) {
                                        val audioManager = ServiceUtil.getAudioManager(context)
                                        audioManager.isBluetoothScoOn = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
            handleBluetoothStateChange()
        }
    }

    private inner class BluetoothConnectionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handleBluetoothStateChange()
        }
    }

    interface BluetoothStateListener {
        fun onBluetoothStateChanged(isAvailable: Boolean)
    }

    companion object {

        private val TAG = BluetoothManager::class.java.simpleName
    }

}
