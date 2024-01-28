package com.example.progetto_ambienti

import android.Manifest
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.progetto_ambienti.ui.home.generaText
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.concurrent.TimeUnit

enum class Operazioni{
    ADDGEO,RMVGEO,DISATTIVA_ALL_GEO,ATTIVA_ALL_GEO,NOTIFY_DWELL
}

val KEY_OPERAZIONE="OPERAZIONE"
val KEY_GEOVECT = "GEO_IDS"

class GeofenceBR :BroadcastReceiver() {
    lateinit var geoClient : GeofencingClient
    lateinit var db : DatabaseHelper
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("GEORECEIVE", "ricevuto intent")
        geoClient = context?.let { LocationServices.getGeofencingClient(it) }!!
        db = DatabaseHelper(context)
        val op = intent?.getStringExtra(KEY_OPERAZIONE)!!
        Log.d("GEORECEIVE", "ricevuto intent di tipo: $op")
        val operazione =Operazioni.valueOf(op)
        when (operazione){
            Operazioni.ADDGEO->aggiungiGeofence(intent,context) //aggiunge la gf al DB, mentre aggiunge al servizio SOLO se sono abilitati
            Operazioni.NOTIFY_DWELL->sendNotifica(intent,context)       //segnale di gf ricevuto
            Operazioni.RMVGEO->rimuoviGeofence(intent)          //rimuove la gf dal DB e dal servizio
            Operazioni.DISATTIVA_ALL_GEO->disattivaServizio()   //rimuove dal servizio le gf ma non dal DB
            Operazioni.ATTIVA_ALL_GEO->attivaServizio(context)         //attiva tutte le gf presenti nel database (avviato solo se prima disattivate)
            else->{
                Log.e("GEOFENCE","??? op intent non ricevuta")
            }
        }
    }

    private fun attivaServizio(context: Context) {
        val posizioni = db.getPosizioni().toList()
        //nessuna posizione salvata e nessuna geofence da attivare
        // (caso primo avvio tutte le posizioni eliminate)
        if(posizioni.isEmpty())
            return

        val geofencesList = mutableListOf<Geofence>()
        for(gf in posizioni){
            geofencesList.add(Geofence.Builder()
                .setRequestId(gf.nome)
                .setCircularRegion(gf.lat, gf.long, DEFAULT_RAGGIO.toFloat())
                .setNotificationResponsiveness(TimeUnit.MINUTES.toMillis(5).toInt())
                .setLoiteringDelay(TimeUnit.MINUTES.toMillis(1).toInt())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build())
        }
        val geoRequest = GeofencingRequest.Builder()
            .addGeofences(geofencesList)
            .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()


        val intentNotify = Intent(context,GeofenceBR::class.java)
        intentNotify.putExtra(KEY_OPERAZIONE,Operazioni.NOTIFY_DWELL.toString())

        val geoIntent =
            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.S)
                PendingIntent.getBroadcast(context,0,intentNotify,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            else
                PendingIntent.getBroadcast(context,0,intentNotify,PendingIntent.FLAG_UPDATE_CURRENT)

        if (ActivityCompat.checkSelfPermission(
                Applicazione.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                Applicazione.getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        geoClient.addGeofences(geoRequest,geoIntent)
    }

    private fun sendNotifica(intent: Intent, context: Context){
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent!!.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e("ERROREGEO", errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            // Get the transition details as a String.
            val geofenceName = triggeringGeofences?.get(0)?.requestId

            // Send notification and log the transition details.
            buildNotification(geofenceName,context)
            Log.i("GEOFENCE", geofenceName!!)
        }
    }

    private fun buildNotification(geofenceName: String?, context: Context) {
        val intentApp =
            Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 11, intentApp,
            PendingIntent.FLAG_IMMUTABLE
        )
        val prodToNotify = db.getProdottiWarn()
        val notyTextQuasiScaduti: String = generaText(prodToNotify)
        val builderQscad = NotificationCompat.Builder(
            Applicazione.getApplicationContext(),
            "NOTIFY_SCADUTI"
        )
            .setSmallIcon(R.drawable.baseline_date_range_24)
            .setContentTitle("Sei a $geofenceName? Ecco quello che hai già a casa!")
            .setContentText(notyTextQuasiScaduti)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notyTextQuasiScaduti)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                Applicazione.getApplicationContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(Applicazione.getApplicationContext())
            .notify(10000, builderQscad.build())
    }

    private fun aggiungiGeofence(intent: Intent,context: Context?) {
        val geoFencesData = intent.getParcelableArrayListExtra<Posizione>(KEY_GEOVECT) ?: return

        if (!AVVISI_POSIZIONE) return

        val geofencesList = mutableListOf<Geofence>()
        for(gf in geoFencesData){
            geofencesList.add(Geofence.Builder()
                .setRequestId(gf.nome)
                .setCircularRegion(gf.lat, gf.long, DEFAULT_RAGGIO.toFloat())
                .setNotificationResponsiveness(TimeUnit.SECONDS.toMillis(1).toInt())
                .setLoiteringDelay(TimeUnit.SECONDS.toMillis(1).toInt())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build())
        }
        val geoRequest = GeofencingRequest.Builder()
            .addGeofences(geofencesList)
            .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_DWELL)
            .build()

        val intentNotify = Intent(context,GeofenceBR::class.java)
        intentNotify.putExtra(KEY_OPERAZIONE,Operazioni.NOTIFY_DWELL.toString())

        val geoIntent =
            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.S)
                PendingIntent.getBroadcast(context,12,intentNotify,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            else
                PendingIntent.getBroadcast(context,12,intentNotify,PendingIntent.FLAG_UPDATE_CURRENT)


//mandatory check anche se le call vengono disattivate se l'utente non ha dato i permessi
        if (ActivityCompat.checkSelfPermission(
                Applicazione.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                Applicazione.getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        Log.d("GEORECEIVE", "geofence creata")

        geoClient.addGeofences(geoRequest,geoIntent).run {
            addOnSuccessListener {
                Log.d("GEORECEIVE", "successo geofence")
            }
            addOnFailureListener{
                Log.d("GEORECEIVE", "errore inserimento geofence")
            }
        }
    }

    private fun rimuoviGeofence(intent: Intent) {
        val geoFenceDaRmv = intent.getStringArrayExtra(KEY_GEOVECT) ?: return
        val geoLists=geoFenceDaRmv.toList()
        geoClient.removeGeofences(geoLists)
    }

    private fun disattivaServizio() {
        val geoFencesAttive = db.getPosizioni().toList()
        val listID= mutableListOf<String>()
        for (p in geoFencesAttive){
            listID.add(p.nome)
        }
        geoClient.removeGeofences(listID)
    }


}