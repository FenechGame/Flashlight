package com.barrera.flashlight

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.widget.Toast

class PhoneStateReceiver : BroadcastReceiver() {

    /**
     *Принимает сообщение о звонке от ОС
     */
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        //поступил звонок
        if (state.equals(TelephonyManager.EXTRA_STATE_RINGING))
            MainActivity.instance.blinkingOn()

        //набор номера при исходящем звонке / разговор
        if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            MainActivity.instance.blinkingOff()
            MainActivity.instance.flashLightOff()
        }
        //сброс звонка
        if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            MainActivity.instance.blinkingOff()
            MainActivity.instance.flashLightOff()
        }
    }
}