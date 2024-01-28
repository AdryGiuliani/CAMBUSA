package com.example.progetto_ambienti.ui.gallery

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.progetto_ambienti.Applicazione
import com.example.progetto_ambienti.DatabaseHelper
import com.example.progetto_ambienti.Ricetta
import com.example.progetto_ambienti.arrayRicette
import com.example.progetto_ambienti.databinding.FragmentRicetteBinding
import com.example.progetto_ambienti.ui.AppTheme
import com.example.progetto_ambienti.ui.Highliter
import com.example.progetto_ambienti.ui.home.ProdottiFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton


class RicetteFragment : Fragment() {
    private var _binding: FragmentRicetteBinding? = null
    private val binding get() = _binding!!
    private var db: DatabaseHelper = DatabaseHelper(Applicazione.getApplicationContext())
    private var selectedRecipe = mutableStateMapOf<String, Ricetta>()
    private lateinit var customCallback: OnBackPressedCallback
    private var selezioneView = ProdottiFragment.VistaSelezione()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(Applicazione.getApplicationContext(), "AAAAAA", Toast.LENGTH_SHORT).show()
    }


        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            Toast.makeText(Applicazione.getApplicationContext(), "BBB", Toast.LENGTH_SHORT).show()

            val ricetteViewModel =
                ViewModelProvider(this).get(RicetteViewModel::class.java)

            _binding = FragmentRicetteBinding.inflate(inflater, container, false)
            val root: View = binding.root

            val textView: TextView = binding.textGallery
            val rmvbt: FloatingActionButton = binding.rmvButton
            rmvbt.hide()

            ricetteViewModel.text.observe(viewLifecycleOwner) {
                textView.text = it
            }

            selectedRecipe.clear()
            selezioneView.bool = false

            binding.componibileRicette.apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme {
                        ElencoCliccabile(ricette = arrayRicette, ricToRemove = selectedRecipe)
                    }
                }
            }
            rmvbt.setOnClickListener {
                for (prod in selectedRecipe.values) {
                    arrayRicette.remove(prod)
                }
                db.rimuoviRicette(selectedRecipe.values)
                rmvbt.hide()
                selezioneView.bool = false
                selectedRecipe.clear()
            }
            return root
        }

        private fun annullaSelezioneCallback(): OnBackPressedCallback {
            val ret = object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    selezioneView.bool = false
                    binding.rmvButton.hide()
                    selectedRecipe.clear()
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

        @Composable
        fun ElencoCliccabile(
            ricette: SnapshotStateList<Ricetta>,
            ricToRemove: SnapshotStateMap<String, Ricetta>
        ) {
            if (ricette.isEmpty())
                binding.textGallery.visibility = View.VISIBLE
            else
                binding.textGallery.visibility = View.GONE

            var selezionato by remember { mutableStateOf(Ricetta().titolo) } //permette alla variabile di aggiornarsi
            //al variare delle condizioni
            val vistaRimozioneLocal = remember { selezioneView }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(
                    ricette, key = { it.titolo }
                ) {
                    cardCliccabile(
                        it,
                        selezionato,
                        ricToRemove,
                        vistaRimozioneLocal
                    )

                }

            }

        }

        @Composable
        @OptIn(ExperimentalFoundationApi::class)
        private fun cardCliccabile(
            oggetto: Ricetta,
            selezionato: String,
            toRimuovere: SnapshotStateMap<String, Ricetta>,
            vistaRimozioneLocal: ProdottiFragment.VistaSelezione
        ) {
            customCallback = annullaSelezioneCallback()
            var espandi by remember { mutableStateOf(false) }
            Surface(modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    enabled = true,
                    onClick = {
                        if (vistaRimozioneLocal.bool) {
                            if (!toRimuovere.contains(oggetto.titolo))
                                toRimuovere[oggetto.titolo] = oggetto
                            else {
                                toRimuovere.remove(oggetto.titolo)
                                if (toRimuovere.isEmpty()) {
                                    customCallback.isEnabled = false
                                    binding.rmvButton.hide()
                                    vistaRimozioneLocal.bool = false
                                }
                            }
                            //     cardSelPerRimozione = !cardSelPerRimozione
                        } else
                            espandi = !espandi
                    },
                    onLongClick = {
                        /*debug toast
                        Toast
                            .makeText(context, "longclick", Toast.LENGTH_SHORT)
                            .show()

                         */
                        if (!vistaRimozioneLocal.bool) {
                            binding.rmvButton.show()
                            customCallback.isEnabled = true
                            vistaRimozioneLocal.bool = true
                        }
                        toRimuovere[oggetto.titolo] = oggetto
                        // cardSelPerRimozione = true
                    }
                ),
                color = if (toRimuovere.contains(oggetto.titolo)) Highliter else Color.Transparent,
                shape = RectangleShape
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 25.dp)
                ) {
                    uiRicetta(r = oggetto, espandi = espandi)
                    // corpo dell'onclick
                    if (selezionato == oggetto.titolo) {
                        Log.d("test", "$oggetto , $selezionato")
                        Toast.makeText(context, "prodotto cliccato", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }

        @Composable
        fun uiRicetta(r: Ricetta, espandi: Boolean) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = r.titolo, Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND)
                        val txtRicetta = toText(r)
                        intent.setType("text/plain")
                        intent.putExtra(Intent.EXTRA_SUBJECT, r.titolo)
                        intent.putExtra(Intent.EXTRA_TEXT, txtRicetta)
                        startActivity(Intent.createChooser(intent, "Condividi con:"))
                    }) {
                        Icon(
                            painter = painterResource(id = androidx.appcompat.R.drawable.abc_ic_menu_share_mtrl_alpha),
                            contentDescription = null
                        )
                    }
                }
                if (espandi) {
                    Text(
                        text = "Difficoltà: " + (r.difficolta),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = "Durata: " + r.durata,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = r.contenuto,
                        modifier = Modifier.padding(8.dp)
                    )

                }
            }
        }

        private fun toText(r: Ricetta): String {
            val sb = StringBuilder()
            sb.append(r.titolo + "\n")
            sb.append("Difficoltà: " + r.difficolta + "\n")
            sb.append("Durata: " + r.durata + "\n")
            sb.append(r.contenuto)
            return sb.toString()
        }
    }

    fun toRicetta(curRicetta: Cursor): Ricetta {
        val ricetta = Ricetta()
        ricetta.titolo = curRicetta.getString(curRicetta.getColumnIndexOrThrow("titolo"))
        ricetta.difficolta = curRicetta.getString(curRicetta.getColumnIndexOrThrow("diffValue"))
        ricetta.durata = curRicetta.getString(curRicetta.getColumnIndexOrThrow("durata"))
        ricetta.contenuto = curRicetta.getString(curRicetta.getColumnIndexOrThrow("testo"))
        return ricetta
    }
