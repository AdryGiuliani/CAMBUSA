package com.example.progetto_ambienti

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.progetto_ambienti.ui.home.generaText
import okhttp3.internal.wait
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class NotificaProdottiWM(appContext : Context, workerParams: WorkerParameters):
    CoroutineWorker(appContext,workerParams) {
    override suspend fun doWork(): Result {
        val prodotti = mutableListOf<Prodotto>()
        val db = DatabaseHelper(Applicazione.getApplicationContext())
        for (p in db.getProdotti())
            prodotti.add(p)
        prodotti.sort()
        if (prodotti.isEmpty() || ActivityCompat.checkSelfPermission(
                Applicazione.getApplicationContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        val scaduti = ArrayList<Prodotto>()
        val inScadenza = ArrayList<Prodotto>()
        for (p in prodotti) {
            val scadMillis = SimpleDateFormat("dd/MM/yyyy").parse(p.scadenza).time
            if ((scadMillis - System.currentTimeMillis()) <= 0) {
                scaduti.add(p)
            } else if ((scadMillis - System.currentTimeMillis()) <= TimeUnit.DAYS.toMillis(
                    SOGLIA_SCAD
                )
            ) {
                inScadenza.add(p)
            }

        }
        val intentApp = Intent(Applicazione.getApplicationContext(), MainActivity::class.java)
            intentApp.flags=Intent.FLAG_ACTIVITY_SINGLE_TOP//evita di creare activity duplicate
        val pendingIntent = PendingIntent.getActivity(
            Applicazione.getApplicationContext(), 10, intentApp,
            PendingIntent.FLAG_IMMUTABLE
        )

        if (scaduti.isNotEmpty()) {
            val notyTextScaduti: String = generaText(scaduti)
            val intentRimuovi =
                Intent(Applicazione.getApplicationContext(), BroadcastCancella::class.java).apply {
                    putExtra(KEY_PROD_RIMOSSI_NOTY, scaduti)
                    action = ContextCompat.getString(
                        Applicazione.getApplicationContext(),
                        R.string.notyButton
                    )
                }
            val pendingRim: PendingIntent = PendingIntent.getBroadcast(
                Applicazione.getApplicationContext(), 12, intentRimuovi,
                PendingIntent.FLAG_IMMUTABLE
            )

            val builderscad = NotificationCompat.Builder(
                Applicazione.getApplicationContext(),
                "NOTIFY_SCADUTI"
            )
                .setSmallIcon(R.drawable.ic_menu_camera)
                .setContentTitle("Sono appena scaduti dei prodotti!")
                .setContentText(notyTextScaduti)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(notyTextScaduti)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .addAction(
                    R.drawable.baseline_delete_24,
                    ContextCompat.getString(
                        Applicazione.getApplicationContext(),
                        R.string.notyButton
                    ), pendingRim
                )
                .setAutoCancel(true)

            ID_NOTIFICA_CANCELLA_SCADUTI++

            NotificationManagerCompat.from(Applicazione.getApplicationContext())
                .notify(ID_NOTIFICA_CANCELLA_SCADUTI, builderscad.build())

        }
        if (inScadenza.isNotEmpty()) {
            val notyTextQuasiScaduti: String = generaText(inScadenza)
            val builderQscad = NotificationCompat.Builder(
                Applicazione.getApplicationContext(),
                "NOTIFY_SCADUTI"
            )
                .setSmallIcon(R.drawable.baseline_date_range_24)
                .setContentTitle("Stanno per scadere dei prodotti!")
                .setContentText(notyTextQuasiScaduti)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(notyTextQuasiScaduti)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            ID_NOTIFICA_QUASI_SCADUTI++
            NotificationManagerCompat.from(Applicazione.getApplicationContext())
                .notify(ID_NOTIFICA_QUASI_SCADUTI, builderQscad.build())
        }
        return Result.success()
    }
}

/*
class ThreadNotificaScadenza(prod : Collection<Prodotto>) : Thread(){
    var prodTrack = ArrayList<ProdFlag>()
    var appenaScaduti=ArrayList<ProdFlag>()
    var quasiScaduti=ArrayList<ProdFlag>()
    var tuttiProd = prod //aliasing della lista prodotti

    class ProdFlag(notificaVicino :Boolean?= false, prod : Prodotto) : Prodotto(prod.nome,prod.scadenza,prod.preferibilmente){
        var notifyVicino = notificaVicino
    }
    @SuppressLint("MissingPermission")
    override fun run() {
        try{
            super.run()
            for(p in  tuttiProd){
                val scadMillis = SimpleDateFormat("dd/MM/yyyy").parse(p.scadenza).time
                if ((scadMillis-System.currentTimeMillis())> TimeUnit.DAYS.toMillis(SOGLIA_SCAD))
                    prodTrack.add(ProdFlag(prod = p))
                else if ((scadMillis-System.currentTimeMillis())<= TimeUnit.DAYS.toMillis(SOGLIA_SCAD)
                    && (scadMillis-System.currentTimeMillis())>0)
                    prodTrack.add(ProdFlag(notificaVicino = true,prod = p))
            }

            while (true) {
                TimeUnit.DAYS.sleep(1)

                val ltTrack = prodTrack.listIterator()
                while (ltTrack.hasNext()) {
                    val p = ltTrack.next()
                    val scadMillis = SimpleDateFormat("dd/MM/yyyy").parse(p.scadenza).time
                    if ((scadMillis - System.currentTimeMillis()) < 0) {
                        ltTrack.remove()
                        appenaScaduti.add(p)
                    } else if ((scadMillis - System.currentTimeMillis()) <= TimeUnit.DAYS.toMillis(
                            SOGLIA_SCAD
                        ) && !p.notifyVicino!!
                    ) {
                        p.notifyVicino = true
                        quasiScaduti.add(p)
                    }

                }
                val intentApp =
                    Intent(Applicazione.getApplicationContext(), MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    Applicazione.getApplicationContext(), 10, intentApp,
                    PendingIntent.FLAG_IMMUTABLE
                )
                if (!appenaScaduti.isEmpty()) {
                    val notyTextScaduti: String = generaText(appenaScaduti)
                    val intentRimuovi = Intent(Applicazione.getApplicationContext(), BroadcastCancella::class.java).apply {
                        putExtra(KEY_PROD_RIMOSSI_NOTY,appenaScaduti)
                        action= ContextCompat.getString(
                            Applicazione.getApplicationContext(),
                            R.string.notyButton
                        )
                    }
                    val pendingRim  : PendingIntent = PendingIntent.getBroadcast(Applicazione.getApplicationContext(),12,intentRimuovi,
                        PendingIntent.FLAG_IMMUTABLE)

                    val builderscad = NotificationCompat.Builder(
                        Applicazione.getApplicationContext(),
                        "NOTIFY_SCADUTI"
                    )
                        .setSmallIcon(R.drawable.ic_menu_camera)
                        .setContentTitle("Hai dei prodotti scaduti!")
                        .setContentText(notyTextScaduti)
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(notyTextScaduti)
                        )
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .addAction(R.drawable.baseline_delete_24,
                            ContextCompat.getString(
                                Applicazione.getApplicationContext(),
                                R.string.notyButton
                            ),pendingRim)
                        .setAutoCancel(true)

                    appenaScaduti.clear()
                    ID_NOTIFICA_CANCELLA_SCADUTI++
                    NotificationManagerCompat.from(Applicazione.getApplicationContext())
                        .notify(ID_NOTIFICA_CANCELLA_SCADUTI, builderscad.build())
                }
                if (!quasiScaduti.isEmpty()) {
                    val notyTextQuasiScaduti: String = generaText(quasiScaduti)
                    val builderQscad = NotificationCompat.Builder(
                        Applicazione.getApplicationContext(),
                        "NOTIFY_SCADUTI"
                    )
                        .setSmallIcon(R.drawable.baseline_date_range_24)
                        .setContentTitle("Stanno per scadere dei prodotti!")
                        .setContentText(notyTextQuasiScaduti)
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(notyTextQuasiScaduti)
                        )
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                    ID_NOTIFICA_QUASI_SCADUTI++
                    NotificationManagerCompat.from(Applicazione.getApplicationContext())
                        .notify(ID_NOTIFICA_QUASI_SCADUTI, builderQscad.build())

                }


            }
        }catch (_:InterruptedException){}
    }

}
}

 */