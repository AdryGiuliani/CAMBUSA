package com.example.progetto_ambienti

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.progetto_ambienti.ui.AppTheme
import com.example.progetto_ambienti.ui.md_theme_dark_onBackground
import com.example.progetto_ambienti.ui.md_theme_light_onBackground


class SettingsActivity : AppCompatActivity() {

    lateinit var impostazioni : SharedPreferences
    var valueScadenza = SOGLIA_SCAD

    override fun onCreate(savedInstanceState: Bundle?,) {
        super.onCreate(savedInstanceState)
        var disattivazioneGeofence =false
        var attivazioneGeofence = false
        impostazioni = Applicazione.getApplicationContext().getSharedPreferences(KEYIMPOSTAZIONI,0)
        val statoIniziale = AVVISI_POSIZIONE
        setContent {
            AppTheme {
                val coloreText = if(isSystemInDarkTheme()) md_theme_dark_onBackground else md_theme_light_onBackground
                val applicabile by remember{mutableStateOf(true)}
                Column(modifier = Modifier.padding(vertical = 16.dp)){
                    selezAvviso(coloreText, applicabile, valueScadenza)
                    Spacer(modifier = Modifier.padding(16.dp))
                    Row(modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                        var statoCheck by remember{mutableStateOf(AVVISI_POSIZIONE)}
                        Text(text = stringResource(id = R.string.togglePos),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = coloreText)
                        Switch(checked = statoCheck,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onCheckedChange = {
                                AVVISI_POSIZIONE= !AVVISI_POSIZIONE

                                //set a true la disattivazione se è stato deselezionata l'opzione
                                //e viceversa per l'attivazione
                                disattivazioneGeofence = !AVVISI_POSIZIONE
                                attivazioneGeofence = AVVISI_POSIZIONE

                                statoCheck= !statoCheck
                                Toast.makeText(this@SettingsActivity, "$AVVISI_POSIZIONE", Toast.LENGTH_SHORT).show()
                                val edit = impostazioni.edit()
                                edit.putBoolean(KEY_POSIZIONE, statoCheck)
                                edit.apply()
                            })
                    }
                    Spacer(modifier = Modifier.padding(vertical = 64.dp))
                    Row(modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ){
                        Button(onClick = {finish()}) {
                            Text(
                                text = stringResource(id = R.string.INDIETRO)
                            )
                        }
                        Button(onClick = {
                            if(applicabile) {
                                val edit = impostazioni.edit()
                                edit.putLong(KEYSCADENZA, valueScadenza)
                                edit.apply()
                                SOGLIA_SCAD=valueScadenza
                                if (AVVISI_POSIZIONE != statoIniziale){
                                    if (disattivazioneGeofence){
                                        Log.d("geofence", "geofence disattivate")
                                        val intent = Intent(Applicazione.getApplicationContext(), GeofenceBR::class.java)
                                        intent.putExtra(KEY_OPERAZIONE, Operazioni.DISATTIVA_ALL_GEO.toString())
                                        intent.also { sendBroadcast(it) }
                                    }else if (attivazioneGeofence){
                                        Log.d("geofence", "geofence attivate")
                                        val intent = Intent(Applicazione.getApplicationContext(), GeofenceBR::class.java)
                                        intent.putExtra(KEY_OPERAZIONE, Operazioni.ATTIVA_ALL_GEO.toString())
                                        intent.also { sendBroadcast(it) }
                                    }
                                }
                                finish()
                            }else
                                Toast.makeText(this@SettingsActivity, "Impossibile salvare, valori non validi", Toast.LENGTH_SHORT).show()
                        }) {
                            Text(
                                text = stringResource(id = R.string.OK)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun selezAvviso(coloreText: Color, applicabile : Boolean, scad : Long) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.impostazioni),
                color = coloreText
            )
        }
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(id = R.string.tresholdScadenza),
                Modifier.padding(16.dp),
                color = coloreText
            )

            var inputok by remember { mutableStateOf(applicabile) }
            var value by remember { mutableStateOf(SOGLIA_SCAD.toString()) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    value = value,
                    onValueChange = {
                        value = it
                        try {
                            inputok= (it.toLong() >= 0)
                            valueScadenza = it.toInt().toLong()
                        } catch (e: NumberFormatException) {
                            inputok = false
                        }
                    },
                    isError = !inputok
                )
            }


        }
    }

}