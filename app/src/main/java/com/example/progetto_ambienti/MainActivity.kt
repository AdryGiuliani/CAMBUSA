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
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.progetto_ambienti.databinding.ActivityMainBinding
import com.google.android.gms.location.Geofence
import java.util.UUID
import java.util.concurrent.TimeUnit


var AUTHCONCESSE=false
var AUTHNOTIFY=false
class MainActivity : AppCompatActivity() {





    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!DB_LETTO){
            //lettura dati secondari
            val uploadWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<DatiGlobalSecondary>()
                    .build()
            WorkManager
                .getInstance(Applicazione.getApplicationContext())
                .enqueue(uploadWorkRequest)
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



    override fun onPause() {
        super.onPause()
        val db =DatabaseHelper(Applicazione.getApplicationContext())
        db.close()
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

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    this.startActivity(intent)
                    return true
                }
            }
            return super.onOptionsItemSelected(item)
        }

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

    private fun avviaWMNotifiche() {
        val uwr=
            PeriodicWorkRequestBuilder<NotificaProdottiWM>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(10, TimeUnit.SECONDS) //for debug purpose dovrebbe essere di 1gg
                .build()
        WorkManager
            .getInstance(Applicazione.getApplicationContext())
            .enqueueUniquePeriodicWork(ID_WMNoty,ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,uwr)
    }

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

    fun creaGeofence(id :String, lat: Double, long : Double, raggio : Float): Geofence{
        return Geofence.Builder().apply {
            setRequestId(id)
            setCircularRegion(lat, long, raggio)
            setNotificationResponsiveness(
                TimeUnit.MINUTES.toMillis(5).toInt()
            ) //riduce il consumo di batteria
            setLoiteringDelay(TimeUnit.MINUTES.toMillis(1).toInt())
            setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
        }
            .build()
    }
}
fun leggiImpostazioni() {
    val preference = Applicazione.getApplicationContext().getSharedPreferences(KEYIMPOSTAZIONI,0)
    SOGLIA_SCAD = preference.getLong(KEYSCADENZA, 10L)
    AVVISI_POSIZIONE=preference.getBoolean(KEY_POSIZIONE, true)
}