package com.example.progetto_ambienti

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.progetto_ambienti.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val contrOption = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){}
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState!=null){
            leggiImpostazioni()
            recuperaListe(savedInstanceState)
        }
        if (!DB_LETTO){
            //lettura dati secondari
            val vmLettura : DatiGlobalSecondary by viewModels()
            lifecycleScope.launch {
                vmLettura.letturaDatiFlow()
            }
            //
        }
        verificaPermessi()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navhostFrag =
            supportFragmentManager.findFragmentById(R.id.navhost_fragment_content_main) as NavHostFragment
        val navController = navhostFrag.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )

        if (navController != null) {
                setupActionBarWithNavController(navController, appBarConfiguration)
                navView.setupWithNavController(navController)
            }


        }

    /**
     * retrieve le liste precedenti se il layout è cambiato in orizzontale senza accedere al db
     */
    private fun recuperaListe(savedInstanceState: Bundle) {
        val prod=savedInstanceState.getParcelableArrayList<Prodotto>(KEY_PRODOTTI_STATE)
        val pos=savedInstanceState.getParcelableArrayList<Posizione>(KEY_POSIZIONI_STATE)
        val ric=savedInstanceState.getParcelableArrayList<Ricetta>(KEY_RICETTE_STATE)
        if(prod==null || pos==null || ric==null) //se uno solo è null vuol dire che non ho modificato io il bundle
            return
        for (p in prod){
            arrayProdotti.add(p)
        }
        for (r in ric){
            arrayRicette.add(r)
        }
        for (ps in pos){
            arrayPosizioni.add(ps)
        }
        DB_LETTO = true
    }


    override fun onPause() {
        super.onPause()
        val db =DatabaseHelper(Applicazione.getApplicationContext())
        db.close()
    }

    /**
     * salva le liste per gestire il caso di rotazione schermo
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val p = ArrayList<Prodotto>()
        val r = ArrayList<Ricetta>()
        val pos = ArrayList<Posizione> ()
        for (p1 in arrayProdotti){
            p.add(p1)
        }
        for (r1 in arrayRicette){
            r.add(r1)
        }
        for (pos1 in arrayPosizioni){
            pos.add(pos1)
        }
        outState.putParcelableArrayList(KEY_PRODOTTI_STATE,p)
        outState.putParcelableArrayList(KEY_POSIZIONI_STATE,pos)
        outState.putParcelableArrayList(KEY_RICETTE_STATE,r)
    }


    /**
     * pulisce la memoria locale alla chiusura dell'app
     */
    override fun onDestroy() {
        arrayProdotti.clear()
        arrayRicette.clear()
        arrayPosizioni.clear()
        DB_LETTO=false
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
            // Inflate the menu; this adds items to the action bar if it is present.
            menuInflater.inflate(R.menu.main, menu)
            return true
        }

        override fun onSupportNavigateUp(): Boolean {
            val navController = findNavController(R.id.navhost_fragment_content_main)
            return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        }


    /**
     * Intent del menù opzioni
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_settings -> {
                    contrOption.launch(Intent(this, SettingsActivity::class.java))
                    return true
                }
            }
            return super.onOptionsItemSelected(item)
        }

    /**
     * verifica ed eventualmente richiede i permessi dell'app, purtroppo è necessario "Permetti sempre" all'accesso della posizione
     * per l'utilizzo di geofence e tale permesso non può essere inserito nel dialog di default nell'app
     */
    private fun verificaPermessi() {
        if (ActivityCompat.checkSelfPermission(
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
            ||
            ActivityCompat.checkSelfPermission(
                Applicazione.getApplicationContext(),
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.ACCESS_FINE_LOCATION",
                    "android.permission.ACCESS_BACKGROUND_LOCATION",
                    "android.permission.POST_NOTIFICATIONS"),
                REQUEST_CODE
            )
        }else{
            AUTHCONCESSE=true
            createNotificationChannel()
            avviaWMNotifiche()
        }
    }

    /**
     * Imposta, se non già attivo, il work manager per gestire le notifiche giornaliere sui prodotti
     */
    private fun avviaWMNotifiche() {
        val uwr=
            PeriodicWorkRequestBuilder<NotificaProdottiWM>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(12, TimeUnit.HOURS)
                .build()
        WorkManager
            .getInstance(Applicazione.getApplicationContext())
            .enqueueUniquePeriodicWork(ID_WMNoty,ExistingPeriodicWorkPolicy.KEEP,uwr)
    }

    /**
     * gestisce il risultato dell'utente alla richiesta permessi
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setMessage(getString(R.string.mexPermessi))
                .setTitle("Permesso negato")
                .setPositiveButton("OK") { dialog, which ->
                    dialog.dismiss()
                }
                .setNegativeButton("AUTORIZZA") { dialog, which ->
                    val intent = Intent()
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", this.packageName, null)
                    intent.setData(uri)
                    this.startActivity(intent)
                    dialog.dismiss()
                }
            val dialog: AlertDialog = builder.create()
            dialog.show()
            if (grantResults[permissions.indexOf("android.permission.POST_NOTIFICATIONS")] == PackageManager.PERMISSION_GRANTED){
            //
                AUTHNOTIFY = true
                createNotificationChannel()
                avviaWMNotifiche()
            }
            else
                WorkManager.getInstance(Applicazione.getApplicationContext()).cancelUniqueWork(
                    ID_WMNoty)
        }
        else{
            AUTHCONCESSE=true
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = NOTY_CHANNEL_ID
            val descriptionText = "warning prodotti"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTY_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}

/**
 * Legge le sharedPreferences
 */
fun leggiImpostazioni() {
    val preference = Applicazione.getApplicationContext().getSharedPreferences(KEYIMPOSTAZIONI,0)
    SOGLIA_SCAD = preference.getLong(KEYSCADENZA, 10L)
    AVVISI_POSIZIONE=preference.getBoolean(KEY_POSIZIONE, true)
}
