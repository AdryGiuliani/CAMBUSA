package com.example.progetto_ambienti

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.example.progetto_ambienti.ui.AppTheme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ActivityRicetta : AppCompatActivity() {

    private val RICETTA_ERROR  = """{
     "titolo": "ERRORE",
     "durata" : "",
     "difficolta" : "",
     "procedimento": ""
   }
        """.trimIndent()

    private val client = OkHttpClient()
    lateinit var db : DatabaseHelper
    var esitoOk =false
    var ingredienti = ArrayList<String>()
    var ricettaToDisplay = mutableStateListOf(Ricetta())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ricette_generator)
        db = DatabaseHelper(Applicazione.getApplicationContext())
        val intent = intent
        ingredienti = intent.getSerializableExtra(KEY_INGREDIENTI) as ArrayList<String>
        val stato : TextView= findViewById(R.id.infoStato)
        val compose : ComposeView =findViewById (R.id.sezione_compose)
        stato.text = getString(R.string.ATTENDI_RICETTA)

        val callback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                val result = Intent()
                setResult(if (esitoOk) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
                finish()
            }
        }
        this.onBackPressedDispatcher.addCallback(
            this, // LifecycleOwner
            callback
        )
        val queryRicetta: String = requestGen()
        try {
            getRisposta(queryRicetta) { res ->
                runOnUiThread{retrieveRicetta(res, ricettaToDisplay)}
            }
        } catch (e: Exception) {
            stato.text = getString(R.string.errore)
        }catch (e : javax.net.ssl.SSLHandshakeException){
            stato.text = getString(R.string.erroreRicetta)
        }

        compose.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    if (ricettaToDisplay.first().titolo == "ERRORE")
                        stato.text = getString(R.string.errore)
                    else if (ricettaToDisplay.first().titolo != ""){
                        esitoOk=true
                        stato.visibility= View.GONE
                        ricetteDisplay(ricettaToDisplay)
                    }
                }
            }
        }

    }


    private fun retrieveRicetta(res: String, ricettaToDisplay: SnapshotStateList<Ricetta>){
        val json = JSONObject(res)
        val titolo = json.getString("titolo")
        val durata = json.getString("durata")
        val diff = json.getString("difficolta")
        val proc = json.getString("procedimento")
        Log.d("ricette", titolo)
        Log.d("ricette", durata)
        Log.d("ricette", diff)
        Log.d("ricette", proc)
        esitoOk=true
        ricettaToDisplay[0]=Ricetta(titolo, durata, diff, proc)
    }

    private fun requestGen(): String {
        val sb =StringBuilder("")
        for (ingr in ingredienti){
            sb.append("$ingr, ")
        }
        return sb.toString()
    }

    @Composable
    fun ricetteDisplay(ricettaGenSnap: SnapshotStateList<Ricetta>) {
        val r = ricettaGenSnap.first()
        Card(Modifier.padding(10.dp)){
            LazyColumn(
                horizontalAlignment = Alignment.Start,
                contentPadding = PaddingValues(16.dp)
            ) {
                item { Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(text = r.titolo, fontWeight = FontWeight.Bold)
                }}

                item { Row {
                    Text(text = "Difficoltà:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(10.dp))
                    Text(text = r.difficolta, fontWeight = FontWeight.Bold)
                } }

                item { Row {
                    Text(text = "Tempo:", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.size(10.dp))
                    Text(text = r.durata, fontWeight = FontWeight.Bold)
                } }
                item { Text(text = r.contenuto) }
            }

            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()){
                Button(onClick = {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this@ActivityRicetta)
                    val txt = EditText(this@ActivityRicetta)
                    txt.setText(r.titolo)
                    builder
                        .setTitle("Inserisci un nome per salvare la ricetta")
                        .setView(txt)
                        .setPositiveButton("SALVA", null)
                        .setNegativeButton("ANNULLA") { dialog, _ ->
                            dialog.dismiss()
                        }

                    val dialog: AlertDialog = builder.create()
                    dialog.setOnShowListener {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            r.titolo=txt.text.toString()
                            if (arrayRicette.contains(r)) {
                                txt.error = "Nome già usato, impossibile aggiungere"
                            } else {
                                try{
                                    if (db.insertRicetta(r.titolo, r.difficolta, r.durata, r.contenuto)){
                                        Toast.makeText(this@ActivityRicetta, "Ricetta salvata!", Toast.LENGTH_SHORT).show()
                                        arrayRicette.add(r)
                                    }else
                                        Toast.makeText(this@ActivityRicetta, "Ricetta non salvata!", Toast.LENGTH_SHORT).show()
                                }catch (e : Exception){
                                    Toast.makeText(this@ActivityRicetta, "Ricetta già salvata!", Toast.LENGTH_SHORT).show()
                                }
                                dialog.dismiss()
                            }
                        }
                    }
                    dialog.show()
                }) {
                    Text(text = "SALVA RICETTA")
                }
                Button(onClick = {
                    onBackPressedDispatcher.onBackPressed()
                }) {
                    Text(text = "INDIETRO")
                }
            }
        }

    }


    fun getRisposta(domanda : String, responso: (String)-> Unit){
        val leng = Locale.current.language
        //
        val apikey= BuildConfig.OPENAI_API_KEY
        val url = "https://api.openai.com/v1/chat/completions"
        var risultato = "Errore impossibile recuperare risposta"
        val requestBody="""{
     "model": "gpt-3.5-turbo-1106",
     "response_format" : {"type" : "json_object"},
     "messages": 
     [
         {
        "role": "system",
        "content": "Rispondi nella seguente lingua: $leng, dati in input n ingredienti genera una ricetta step-by-step con i seguenti campi json: titolo, difficolta, durata, quantita necessarie e procedimento (in un unico object), se non rilevi ingredienti genera comunque una ricetta di errore"
      },
      {
        "role": "user",
        "content": "$domanda"
      }
      ],
     "temperature": 1.2
   }
        """.trimIndent()
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apikey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("errore", "Errore di connessione", e)
                responso("errore nella richiesta")
            }

            override fun onResponse(call: Call, response: Response) {
                val body= response.body?.string()
                Log.d("successo", "Connessione ok")
                if (body != null) {
                    Log.v("data",body)
                }
                else{
                    Log.v("data","empty")
                }
                try {
                    val json = JSONObject(body)
                    Log.d("data", json.toString())
                    val jsonlist: JSONArray = json.getJSONArray("choices")
                    Log.d("data", jsonlist.toString())
                    val info = jsonlist.getJSONObject(0)
                    Log.d("data", info.toString())
                    val mex = info.getJSONObject("message")
                    Log.d("data", mex.toString())
                    val contenuto = mex.getString("content")
                    Log.d("data", contenuto.toString())
                    //val testoRisultato = mexRisultato.ge
                    responso(contenuto)
                }catch (e : JSONException){
                   responso(RICETTA_ERROR)
                }

            }})
    }

}