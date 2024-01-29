package com.example.progetto_ambienti

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.progetto_ambienti.ui.home.ProdottiFragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.random.Random

//aggiornate all'avvio della mainActivity
var ID_NOTIFICA_CANCELLA_SCADUTI = Random.nextInt()
var ID_NOTIFICA_QUASI_SCADUTI= Random.nextInt()
val ID_WMNoty="WMNotifiche"
val NOTY_CHANNEL_ID = "NOTIFY_SCADUTI"
var AVVISI_POSIZIONE = true
var DB_LETTO=false
var AUTHCONCESSE=false
var AUTHNOTIFY=false
val KEY_POSIZIONE="preferencePosizione"
val KEY_POS_AGGIUNTI = "aggiuntiPos"
val KEY_POS_PREC = "posizioniAttuali"
val REQUEST_CODE=9
val DEFAULT_RAGGIO=50.0
val KEY_PROD_AGGIUNTI = "aggiuntiProd"
val KEY_PROD_RIMOSSI_NOTY = "rimuoviDaNotificaProd"
val KEY_INGREDIENTI = "ingredienti"
val KEYIMPOSTAZIONI = "impostazioni"
val KEYSCADENZA = "impostazioni"
var SOGLIA_SCAD by mutableLongStateOf(10L)
var variazioneProdotti = false
var arrayProdotti = mutableStateListOf<Prodotto>()
var arrayRicette = mutableStateListOf<Ricetta>()
var arrayPosizioni = mutableStateListOf<Posizione>()

class DatiGlobalSecondary: ViewModel(){

    /**
     * alternativa di lettura senza animazione flow
     */
     fun letturaDati(){
         viewModelScope.launch {
             try {
                 leggiImpostazioni()
                 val db = DatabaseHelper(Applicazione.getApplicationContext())
                 for (p in db.getProdotti())
                     arrayProdotti.add(p)
                 arrayProdotti.sort()
                 variazioneProdotti =
                     false //se ho riaperto l'app non serve riaggiornare la vista dei prodotti
                 for (r in db.getRicette())
                     arrayRicette.add(r)
                 for (p in db.getPosizioni())
                     arrayPosizioni.add(p)
                 db.close()
                 DB_LETTO = true
             }catch (e :Exception){
                 Log.e("data","errore nel recupero dei dati")
                 Toast.makeText(Applicazione.getApplicationContext(),"Errore nella lettura dati salvati",Toast.LENGTH_SHORT).show()
             }
         }
        }

    /**
     * legge i dati in maniera asincrona mediante flusso
     */
    fun letturaDatiFlow(){
        viewModelScope.launch {
            try {
                val db = DatabaseHelper(Applicazione.getApplicationContext())
                leggiImpostazioni()
                variazioneProdotti = false
                val prod =db.getProdotti().sorted()
                val flow = prod.asFlow().onEach { delay(150L) }
                flow.collect{p -> arrayProdotti.add(p) }
                //se ho riaperto l'app non serve riaggiornare la vista dei prodotti
                for (r in db.getRicette())
                    arrayRicette.add(r)
                for (p in db.getPosizioni())
                    arrayPosizioni.add(p)
                db.close()
                DB_LETTO = true
            }catch (e :Exception){
                Log.e("data","errore nel recupero dei dati")
                Toast.makeText(Applicazione.getApplicationContext(),"Errore nella lettura dati salvati",Toast.LENGTH_SHORT).show()
            }
        }
    }
}
