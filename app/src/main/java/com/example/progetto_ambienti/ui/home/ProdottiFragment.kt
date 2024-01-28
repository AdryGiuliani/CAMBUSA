package com.example.progetto_ambienti.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getString
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.progetto_ambienti.ui.AppTheme
import com.example.progetto_ambienti.ui.md_theme_dark_surfaceTint
import com.example.progetto_ambienti.ui.md_theme_light_error
import com.example.progetto_ambienti.AUTHCONCESSE
import com.example.progetto_ambienti.AUTHNOTIFY
import com.example.progetto_ambienti.ActivityAggiuntaProdotto
import com.example.progetto_ambienti.ActivityRicetta
import com.example.progetto_ambienti.Applicazione
import com.example.progetto_ambienti.BroadcastCancella
import com.example.progetto_ambienti.DatabaseHelper
import com.example.progetto_ambienti.KEY_INGREDIENTI
import com.example.progetto_ambienti.KEY_PROD_AGGIUNTI
import com.example.progetto_ambienti.KEY_PROD_RIMOSSI_NOTY
import com.example.progetto_ambienti.MainActivity
import com.example.progetto_ambienti.Prodotto
import com.example.progetto_ambienti.R
import com.example.progetto_ambienti.SOGLIA_SCAD
import com.example.progetto_ambienti.arrayProdotti
import com.example.progetto_ambienti.databinding.FragmentProdottiBinding
import com.example.progetto_ambienti.ui.Highliter
import com.example.progetto_ambienti.variazioneProdotti
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.random.Random

//id dell'extra tra main e aggiunta
//aggiornate all'avvio della main activity

class ProdottiFragment : Fragment() {

    private var _binding: FragmentProdottiBinding? = null
    private var curDate = System.currentTimeMillis()
    private lateinit var contractAggiuntaProdotto: ActivityResultLauncher<Intent>
    private lateinit var contractRicetta: ActivityResultLauncher<Intent>

    //lista "tenuta d'occhio" dal compiler dell'UI in caso di aggiunte
    private var db: DatabaseHelper = DatabaseHelper(Applicazione.getApplicationContext())
    private var selectedProd = mutableStateMapOf<String, Prodotto>()

    class VistaSelezione {
        var bool by mutableStateOf(false)
    }

    private var rimozioneView = VistaSelezione()
    private lateinit var customCallback: OnBackPressedCallback
    private val binding get() = _binding!!

    //dichiaro nell'oncreate il contract per aggiungere nuovi prodotti
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contractAggiuntaProdotto =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    for (prod in result.data?.getParcelableArrayListExtra<Prodotto>(
                        KEY_PROD_AGGIUNTI
                    )!!) {
                        arrayProdotti.add(prod)
                    }
                    arrayProdotti.sort()
                    Toast.makeText(context, "Prodotto correttamente aggiunto", Toast.LENGTH_SHORT)
                        .show()

                } else
                    Toast.makeText(context, "Prodotto non aggiunto", Toast.LENGTH_SHORT).show()
            }
        contractRicetta =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Toast.makeText(context, "Ricetta correttamente creata", Toast.LENGTH_SHORT)
                        .show()

                } else
                    Toast.makeText(
                        context,
                        "Errore nella generazione delle ricetta",
                        Toast.LENGTH_SHORT
                    ).show()
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val prodottiViewModel =
            ViewModelProvider(this)[ProdottiViewModel::class.java]



        _binding = FragmentProdottiBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val addSelected: FloatingActionButton = binding.aggiungi
        val rmSelected: FloatingActionButton = binding.rmvSelected
        val creaRicetta: FloatingActionButton = binding.creaRecipe
        rmSelected.hide()
        creaRicetta.hide()

        prodottiViewModel.text.observe(viewLifecycleOwner) {
            binding.textHome.text = it
        }
        selectedProd.clear()
        rimozioneView.bool = false


        binding.componibile.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    ElencoCliccabile(prodotti = arrayProdotti, prodToRemove = selectedProd)
                }
            }
        }

        addSelected.setOnClickListener {
            contractAggiuntaProdotto.launch(Intent(activity, ActivityAggiuntaProdotto::class.java))
        }

        rmSelected.setOnClickListener {
            val toRMV = selectedProd.values
            for (prod in selectedProd.values) {
                arrayProdotti.remove(prod)
            }
            db.rimuoviProdotti(toRMV)
            creaRicetta.hide()
            rmSelected.hide()
            addSelected.show()
            rimozioneView.bool = false
            selectedProd.clear()
        }

        creaRicetta.setOnClickListener {
            val ingredienti = ArrayList<String>()
            for (nomeProd in selectedProd.keys) {
                ingredienti.add(nomeProd)
            }
            val intentRicetta = Intent(activity, ActivityRicetta::class.java)
            intentRicetta.putExtra(KEY_INGREDIENTI, ingredienti)
            contractRicetta.launch(intentRicetta)
        }
        return root
    }

    override fun onResume() {
        super.onResume()
        //gestone caso eventuali prodotti eliminati da notifica
        if (variazioneProdotti) {
            MainScope().launch {
                val pUpdated = db.getProdotti()
                val lit = arrayProdotti.listIterator()
                while (lit.hasNext()) {
                    val p = lit.next()
                    if (!pUpdated.contains(p))
                        lit.remove()
                }
            }
            variazioneProdotti = false
        }
    }

    private fun annullaSelezioneCallback(): OnBackPressedCallback {
        val ret = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                rimozioneView.bool = false
                binding.aggiungi.show()
                binding.rmvSelected.hide()
                binding.creaRecipe.hide()
                selectedProd.clear()
                this.isEnabled = false
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(), // LifecycleOwner
            ret
        )
        return ret
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Crea un elenco di prodotti sottoforma di CARD da poter cliccare per ricevere più informazioni
     */
    @Composable
    fun ElencoCliccabile(
        prodotti: SnapshotStateList<Prodotto>,
        prodToRemove: SnapshotStateMap<String, Prodotto>
    ) {
        if (prodotti.isEmpty())
            binding.textHome.visibility = View.VISIBLE
        else
            binding.textHome.visibility = View.GONE

        if (ActivityCompat.checkSelfPermission(
                Applicazione.getApplicationContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            /*
            if (threadNotifica != null) {
                threadNotifica?.interrupt()
                threadNotifica = ThreadNotificaScadenza(arrayProdotti)
                threadNotifica?.isDaemon = false
                threadNotifica?.start()
            }
*/
        }

        var selezionato by remember { mutableStateOf(Prodotto().nome) } //permette alla variabile di aggiornarsi
        //al variare delle condizioni
        //var daRimuovere = mutableStateOf(toremoveselected)
        val vistaRimozioneLocal = remember { rimozioneView }
        val indiceCliccato = { index: String -> selezionato = index }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            items(
                prodotti, key = { it -> it.nome }
            ) {
                cardCliccabile(it, indiceCliccato, selezionato, prodToRemove, vistaRimozioneLocal)

            }

        }
    }

    /**
     * Crea una singola card cliccabile associata al prodotto passato come argomento
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun cardCliccabile(
        oggetto: Prodotto,
        cliccato: (String) -> Unit,
        selezionato: String,
        toRimuovere: SnapshotStateMap<String, Prodotto>,
        vistaRimozioneLocal: VistaSelezione
    ) {

        customCallback = annullaSelezioneCallback()
        var espandi by remember { mutableStateOf(false) }
        Surface(modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                enabled = true,
                onClick = {
                    Toast
                        .makeText(context, "click", Toast.LENGTH_SHORT)
                        .show()
                    if (vistaRimozioneLocal.bool) {
                        if (!toRimuovere.contains(oggetto.nome))
                            toRimuovere[oggetto.nome] = oggetto
                        else {
                            toRimuovere.remove(oggetto.nome)
                            if (toRimuovere.isEmpty()) {
                                customCallback.isEnabled = false
                                binding.aggiungi.show()
                                binding.rmvSelected.hide()
                                binding.creaRecipe.hide()
                                vistaRimozioneLocal.bool = false
                            }
                        }
                        //     cardSelPerRimozione = !cardSelPerRimozione
                    } else
                        espandi = !espandi
                },
                onLongClick = {
                    Toast
                        .makeText(context, "longclick", Toast.LENGTH_SHORT)
                        .show()
                    if (!vistaRimozioneLocal.bool) {
                        binding.aggiungi.hide()
                        binding.rmvSelected.show()
                        binding.creaRecipe.show()
                        customCallback.isEnabled = true
                        vistaRimozioneLocal.bool = true
                    }
                    toRimuovere[oggetto.nome] = oggetto
                    // cardSelPerRimozione = true
                }
            ),
            color = if (toRimuovere.contains(oggetto.nome)) Highliter else Color.Transparent,
            /*contentColor = if (vistaRimozione)
                                if (isSystemInDarkTheme())
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    light_onHighlighter
                            else
                                MaterialTheme.colorScheme.onSurface,
             */
            shape = RectangleShape
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 25.dp)
            ) {
                uiProdotto(p = oggetto, espandi = espandi)
                // corpo dell'onclick
                if (selezionato == oggetto.nome) {
                    Log.d("test", "$oggetto , $selezionato")
                    Toast.makeText(context, "prodotto cliccato", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    enum class stato {
        scaduto, breve, lontano
    }


    @Composable
    fun uiProdotto(p: Prodotto, espandi: Boolean) {
        val errColor = md_theme_light_error
        val warnColor = md_theme_dark_surfaceTint
        val scadMillis = SimpleDateFormat("dd/MM/yyyy").parse(p.scadenza).time
        var s = stato.lontano
        if (scadMillis - curDate < 0L)
            s = stato.scaduto
        else if (scadMillis - curDate < TimeUnit.DAYS.toMillis(SOGLIA_SCAD))
            s = stato.breve
        val vicinanzaScadenza by remember { mutableStateOf(s) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = p.nome, Modifier.padding(8.dp),
                color = if (vicinanzaScadenza == stato.scaduto)
                    if (!p.preferibilmente)
                        errColor
                    else
                        warnColor
                else
                    LocalContentColor.current
            )
            if (espandi) {
                Row(horizontalArrangement = Arrangement.End) {
                    Text(
                        text = if (p.preferibilmente) stringResource(id = R.string.preferibilmente) else stringResource(
                            id = R.string.entro
                        ),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = p.scadenza,
                        color = if (vicinanzaScadenza == stato.breve || vicinanzaScadenza == stato.scaduto) md_theme_light_error else LocalContentColor.current,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }


}
fun generaText(l: List<Prodotto>): String {
    val sb =StringBuilder()
    for (p in l){
        sb.append("${p.nome} \t ${p.scadenza} \n")
    }
    return sb.toString()
}

fun generaText(l: Collection<Prodotto>): String {
    val sb =StringBuilder()
    for (p in l){
        sb.append("${p.nome} \t ${p.scadenza} \n")
    }
    return sb.toString()
}

fun toProdotto(curProdotti: Cursor): Prodotto {
    val prodotto = Prodotto()
        prodotto.nome = curProdotti.getString(curProdotti.getColumnIndexOrThrow("nome"))
        prodotto.scadenza = curProdotti.getString(curProdotti.getColumnIndexOrThrow("scadenza"))
        prodotto.preferibilmente= curProdotti.getInt(curProdotti.getColumnIndexOrThrow("preferibilmente"))==1
    return prodotto
}


