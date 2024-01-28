package com.example.progetto_ambienti

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Riattivazione delle geofences in caso di riavvio del dispositivo (vengono disattivate)
 */
class RiavvioBR : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.action != Intent.ACTION_REBOOT) return
        val intentSetup = Intent(Applicazione.getApplicationContext(), GeofenceBR::class.java)
        intent.putExtra(KEY_OPERAZIONE,Operazioni.ATTIVA_ALL_GEO)
        intentSetup.also {context?.sendBroadcast(intentSetup)}
    }
}