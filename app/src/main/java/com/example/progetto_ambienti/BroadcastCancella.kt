package com.example.progetto_ambienti

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class BroadcastCancella : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        NotificationManagerCompat.from(Applicazione.getApplicationContext()).cancel(
            ID_NOTIFICA_CANCELLA_SCADUTI)

        val darimuovere = intent?.getParcelableArrayListExtra<Prodotto>(KEY_PROD_RIMOSSI_NOTY)
        val db = DatabaseHelper(Applicazione.getApplicationContext())
        if (darimuovere != null) {
            variazioneProdotti=true
            db.rimuoviProdotti(darimuovere.toMutableList())
            db.close()
        }
    }
}