package com.example.progetto_ambienti

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.progetto_ambienti.ui.AppTheme
import com.example.progetto_ambienti.ui.home.ProdottiFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

class ActivityAggiuntaProdotto : ComponentActivity() {

    lateinit var db : DatabaseHelper
    var esitoOk =false
    var prodottiAggiunti = ArrayList<Prodotto>()
    class VistaSelezione() {
        var bool by mutableStateOf(false)
    }
    private var selezioneView = ProdottiFragment.VistaSelezione()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db =DatabaseHelper(context = Applicazione.getApplicationContext())
        setContent {
            AppTheme {
                LayoutAggiunta()
            }

        }
        val callback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                val result = intent.putExtra(KEY_PROD_AGGIUNTI, prodottiAggiunti)
                setResult(if(esitoOk) Activity.RESULT_OK  else Activity.RESULT_CANCELED, result)
                finish()
            }
        }
        this.onBackPressedDispatcher.addCallback(
            this, // LifecycleOwner
            callback
        )
    }


    @Composable
    fun LayoutAggiunta(){
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            FormAggiunta()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FormAggiunta() {
        var nomeProd by remember { mutableStateOf("") }

        var opendialog by remember { mutableStateOf(false) }
        var openMenu by remember { mutableStateOf(false) }
        val preferibilmente = stringResource(id = R.string.preferibilmente)
        val entro = stringResource(id = R.string.entro)
        val defautlData = convertMillisToDate(System.currentTimeMillis())
        var pref by remember { mutableStateOf(entro) }

        val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis+ TimeUnit.DAYS.toMillis(1) >= (System.currentTimeMillis())
            }
        })
        var dataSelect by remember {
            mutableStateOf(datePickerState.selectedDateMillis?.let {
                convertMillisToDate(it)
            } ?: defautlData)
        }

        Card(modifier = Modifier.padding(20.dp,20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(15.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = stringResource(id = R.string.titoloNuovoProdotto),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )


                Spacer(modifier = Modifier.padding(5.dp))

                Text(
                    text = stringResource(id = R.string.Nomeprodotto),
                    style = MaterialTheme.typography.bodyLarge
                )

                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black),
                    value = nomeProd,
                    onValueChange = { nomeProd = it },
                    placeholder = { Text(text = "e.g Uova") },
                    singleLine = true
                )

                //seconda riga
                Spacer(modifier = Modifier.padding(5.dp))
                Text(
                    text = stringResource(id = R.string.datascadenza),
                    style = MaterialTheme.typography.bodyLarge
                )

                Card(
                    onClick = { opendialog = true },
                    Modifier
                        .fillMaxSize()
                        .height(50.dp)
                ) {
                    Text(text = dataSelect , Modifier.padding(16.dp))
                }
                // selettore data
                Spacer(modifier = Modifier.padding(5.dp))
                Text(
                    text = stringResource(id = R.string.tiposcadenza),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (opendialog) {
                    DatePickerDialog(
                        onDismissRequest = {
                            dataSelect = datePickerState.selectedDateMillis?.let {
                                convertMillisToDate(it)
                            } ?: ""
                            opendialog = false
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                dataSelect = datePickerState.selectedDateMillis?.let {
                                    convertMillisToDate(it)
                                } ?: ""
                                opendialog = false
                            }) {
                                Text(text = "OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                dataSelect = datePickerState.selectedDateMillis?.let {
                                    convertMillisToDate(it)
                                } ?: ""
                                opendialog = false
                            }) {
                                Text(text = "CHIUDI")
                            }
                        }
                    ) {
                        DatePicker(
                            state = datePickerState,
                            title = { Text(text = stringResource(id = R.string.selscadDate), Modifier.padding(10.dp)) })
                    }

                }
                // terza riga
                Spacer(modifier = Modifier.padding(5.dp))
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExposedDropdownMenuBox(
                        expanded = openMenu,
                        onExpandedChange = { openMenu = it }) {
                        TextField(
                            value = pref,
                            placeholder = { Text(text = stringResource(id = R.string.tiposcadenza)) },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = openMenu) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = openMenu,
                            onDismissRequest = { openMenu = false }) {
                            DropdownMenuItem(text = { Text(text = preferibilmente) },
                                onClick = {
                                    pref = preferibilmente
                                    openMenu = false
                                })
                            DropdownMenuItem(text = { Text(text = entro) },
                                onClick = {
                                    pref = entro
                                    openMenu = false
                                })
                        }
                    }
                }

                //bottone conferma e indietro
                Spacer(modifier = Modifier.padding(5.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                    Button(onClick = { onBackPressedDispatcher.onBackPressed()}) {
                        Text(text = stringResource(id = R.string.INDIETRO))
                    }
                    Button(onClick = {
                        if (verificaInseriti(nomeProd)) {
                            try {
                                if (!db.insertProdotto(nomeProd, dataSelect, pref == preferibilmente))
                                    Toast.makeText(
                                        applicationContext,
                                        "Nome prodotto già inserito",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                else {
                                    esitoOk = true
                                    prodottiAggiunti.add(
                                        Prodotto(
                                            nomeProd,
                                            dataSelect,
                                            pref == preferibilmente
                                        )
                                    )
                                    Toast.makeText(
                                        applicationContext,
                                        "Salvataggio su database completato",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    applicationContext,
                                    "Errore nel salvataggio su database",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        }
                        else{

                            Toast.makeText(
                                applicationContext,
                                "Inserisci tutti i dati richiesti",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Text(text = "INSERISCI")
                    }
                }
            }
        }
    }

    private fun verificaInseriti(nomeProd: String): Boolean {
        return nomeProd != ""
    }

    private fun convertMillisToDate(millis: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy")
        return formatter.format(Date(millis))
    }
}