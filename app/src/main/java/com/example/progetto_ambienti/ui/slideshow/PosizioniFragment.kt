package com.example.progetto_ambienti.ui.slideshow

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.progetto_ambienti.AUTHCONCESSE
import com.example.progetto_ambienti.AVVISI_POSIZIONE
import com.example.progetto_ambienti.ActivityNewPosizione
import com.example.progetto_ambienti.Applicazione
import com.example.progetto_ambienti.DatabaseHelper
import com.example.progetto_ambienti.GeofenceBR
import com.example.progetto_ambienti.KEY_GEOVECT
import com.example.progetto_ambienti.KEY_OPERAZIONE
import com.example.progetto_ambienti.KEY_POS_AGGIUNTI
import com.example.progetto_ambienti.KEY_POS_PREC
import com.example.progetto_ambienti.Operazioni
import com.example.progetto_ambienti.Posizione
import com.example.progetto_ambienti.R
import com.example.progetto_ambienti.arrayPosizioni
import com.example.progetto_ambienti.databinding.FragmentPosizioniBinding
import com.example.progetto_ambienti.ui.AppTheme
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices


class PosizioniFragment : Fragment(){


    lateinit var geoClient : GeofencingClient
    //var geoFencesAttive = ArrayList<String>()
    private var _binding: FragmentPosizioniBinding? = null
    private val binding get() = _binding!!
    private var db : DatabaseHelper = DatabaseHelper(Applicazione.getApplicationContext())
    private lateinit var  contractAggiuntaPos : ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geoClient =
            LocationServices.getGeofencingClient(Applicazione.getApplicationContext())
        if (!AUTHCONCESSE and
            (ActivityCompat.checkSelfPermission(
                Applicazione.getApplicationContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    ||
                    ActivityCompat.checkSelfPermission(
                        Applicazione.getApplicationContext(),
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                    ||
                    ActivityCompat.checkSelfPermission(
                        Applicazione.getApplicationContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                    ))
        {

            requireActivity().supportFragmentManager.popBackStack()
            Toast.makeText(Applicazione.getApplicationContext(), "Funzioni di localizzazione non disponibili", Toast.LENGTH_SHORT).show()
        }



        contractAggiuntaPos =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    //val geofenceDaAggiungere= mutableListOf<String>()
                    val intentAggiunta = Intent(Applicazione.getApplicationContext(),GeofenceBR::class.java)
                    val posDaAdd = result.data?.getParcelableArrayListExtra<Posizione>(KEY_POS_AGGIUNTI)!!
                    for (pos in posDaAdd){
                        arrayPosizioni.add(pos)
                    }
                    if(AVVISI_POSIZIONE){
                        intentAggiunta.putExtra(KEY_OPERAZIONE, Operazioni.ADDGEO.toString())
                        intentAggiunta.putParcelableArrayListExtra(KEY_GEOVECT,posDaAdd)
                        Applicazione.getApplicationContext().sendBroadcast(intentAggiunta)
                    }
                    Toast.makeText(context, "Posizione correttamente aggiunta", Toast.LENGTH_SHORT)
                        .show()
                } else
                    Toast.makeText(context, "Posizione non aggiunta", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if(!AVVISI_POSIZIONE)
            Toast.makeText(Applicazione.getApplicationContext(), "Notifiche di posizione disattivate, riattivale nelle impostazioni", Toast.LENGTH_SHORT).show()

        val posizioniViewModel =
            ViewModelProvider(this)[PosizioniViewModel::class.java]

        _binding = FragmentPosizioniBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val textView: TextView = binding.textSlideshow

        posizioniViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }


        val newPos = binding.addPosizione
        newPos.setOnClickListener {
            val intent = Intent(activity,ActivityNewPosizione::class.java)
            intent.putExtra(KEY_POS_PREC, ArrayList(arrayPosizioni.toList()))
            contractAggiuntaPos.launch(intent)
        }

        binding.posizioniCompose.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    Elenco(posizioni = arrayPosizioni)
                }
            }
        }
        return root
    }

    @Composable
    fun Elenco(posizioni: SnapshotStateList<Posizione>) {
        if(posizioni.isEmpty())
            binding.textSlideshow.visibility=View.VISIBLE
        else
            binding.textSlideshow.visibility=View.GONE

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(
                posizioni, key = {it.nome}
            ) {
                cardCliccabile(it)

            }

        }
    }
    @Composable
    fun cardCliccabile(pos: Posizione) {

        var espandi by remember{ mutableStateOf(false) }
        Card(
            modifier = Modifier
                .padding(horizontal = 25.dp)
                .fillMaxWidth(),
            onClick = { espandi=!espandi},
        ) {
            Column (verticalArrangement = Arrangement.SpaceEvenly){

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = pos.nome, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    IconButton(onClick = {
                        val intentRim = Intent(Applicazione.getApplicationContext(),GeofenceBR::class.java)
                        intentRim.putExtra(KEY_OPERAZIONE, Operazioni.RMVGEO.toString())
                        val transfer = arrayOf(pos.nome)
                        intentRim.putExtra(KEY_GEOVECT,transfer)
                        Applicazione.getApplicationContext().sendBroadcast(intentRim)
                        arrayPosizioni.remove(pos)
                        db.rimuoviPosizioni(listOf(pos))
                    }){
                        Icon(painter = painterResource(id = R.drawable.baseline_delete_24),contentDescription = null)
                    }
                }

                if (espandi)
                    Text(text = "Indirizzo:\n ${pos.indirizzo}", modifier = Modifier.padding(16.dp))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

/**
 * genera Posizione da Cursore di tupla posizioni
 */
fun toPosizione(curPosizione: Cursor): Posizione {
   val posizione = Posizione()
   posizione.nome = curPosizione.getString(curPosizione.getColumnIndexOrThrow("nome"))
   posizione.lat= curPosizione.getDouble(curPosizione.getColumnIndexOrThrow("lat"))
   posizione.long= curPosizione.getDouble(curPosizione.getColumnIndexOrThrow("long"))
    posizione.indirizzo=curPosizione.getString(curPosizione.getColumnIndexOrThrow("ind"))
   return posizione
}