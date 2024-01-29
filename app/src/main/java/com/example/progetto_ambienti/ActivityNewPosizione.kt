package com.example.progetto_ambienti

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException


class ActivityNewPosizione : AppCompatActivity(), OnMapReadyCallback ,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener {

    lateinit var db : DatabaseHelper
    lateinit var searchView: SearchView
    private lateinit var mappa : GoogleMap
    private lateinit var mappaView : View
    private var esitoOk =false
    var posAggiunte = ArrayList<Posizione>()
    var vecchiePos = ArrayList<Posizione>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vecchiePos= intent.getParcelableArrayListExtra(KEY_POS_PREC)!!
        db =DatabaseHelper(context = Applicazione.getApplicationContext())
        setContentView(R.layout.activity_aggiunta_pos_layout)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mappaView= mapFragment!!.requireView()

        mapFragment.getMapAsync(this)

        searchView = findViewById(R.id.idSearchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // on below line we are getting the
                // location name from search view.
                val location = searchView.query.toString()
                // below line is to create a list of address
                // where we will store the list of all address.
                var addressList: List<Address>? = listOf()

                // checking if the entered location is null or not.
                if (location != null || location == "") {
                    // on below line we are creating and initializing a geo coder.
                    val geocoder = Geocoder(this@ActivityNewPosizione)
                    try {
                        // on below line we are getting location from the
                        // location name and adding that location to address list.
                        addressList = geocoder.getFromLocationName(location, 1)
                        if (addressList.isNullOrEmpty()){
                            Toast.makeText(this@ActivityNewPosizione,"Nessun risultato",Toast.LENGTH_SHORT).show()
                            return false
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    val address: Address = addressList!![0]

                    // on below line we are creating a variable for our location
                    // where we will add our locations latitude and longitude.
                    val latLng = LatLng(address.getLatitude(), address.getLongitude())

                    // on below line we are adding marker to that position.
                    // below line is to animate camera to that position.
                    mappa.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13.5f))
                }
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })

        val callback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                val result = intent.putExtra(KEY_POS_AGGIUNTI, posAggiunte)
                if (posAggiunte.isNotEmpty()){
                    esitoOk=db.insertPosizioni(posAggiunte)
                }
                setResult(if(esitoOk) Activity.RESULT_OK  else Activity.RESULT_CANCELED, result)
                finish()
            }
        }
        this.onBackPressedDispatcher.addCallback(
            this, // LifecycleOwner
            callback
        )
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        LocationServices.getFusedLocationProviderClient(Applicazione.getApplicationContext()).lastLocation.addOnSuccessListener {
            val posLATLONG= LatLng(it.latitude, it.longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(posLATLONG,18F))
        }
        mappa=map
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        val view1= mappaView.findViewById<View?>(Integer.parseInt("1")).parent as View
        val locButton = view1.findViewById<View>(Integer.parseInt("2"))
        val layoutParams =locButton.layoutParams as RelativeLayout.LayoutParams
        // posiziono il tasto per recuperare la posizione in basso a dx
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        layoutParams.setMargins(0, 0, 16, 16)
        for (pos in vecchiePos){
            map.addMarker(
                MarkerOptions().position(LatLng(pos.lat,pos.long))
                    .title(pos.nome)
            )
            map.addCircle(
                CircleOptions().center(LatLng(pos.lat,pos.long))
                    .radius(DEFAULT_RAGGIO)
            )
        }

        map.setOnMapLongClickListener {
            Toast.makeText(this, "longclick", Toast.LENGTH_SHORT).show()
            val newPos = Posizione(lat = it.latitude, long = it.longitude)

            val latlong = it
            //recupero indirizzo da coordinate
            var addressList: List<Address>? = listOf()

            // on below line we are creating and initializing a geo coder.
            val geocoder = Geocoder(this@ActivityNewPosizione)
            try {
                // on below line we are getting location from the
                // location name and adding that location to address list.
                addressList = geocoder.getFromLocation(it.latitude, it.longitude, 1)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val address: Address = addressList!![0]

            newPos.indirizzo = address.getAddressLine(0)


            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            val txt = EditText(this)
            builder
                .setTitle("Inserisci un nome per salvare il luogo")
                .setView(txt)
                .setPositiveButton("SALVA", null)
                .setNegativeButton("ANNULLA") { dialog, which ->
                    dialog.dismiss()
                }

            val dialog: AlertDialog = builder.create()
            dialog.setOnShowListener {

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    newPos.nome = txt.text.toString()
                    if (vecchiePos.contains(newPos) or posAggiunte.contains(newPos)) {
                        txt.error = "Nome posizione già presente, impossibile aggiungere"
                    } else {
                        map.addMarker(
                            MarkerOptions().position(latlong).title(newPos.nome)
                        )
                        map.addCircle(
                            CircleOptions().center(LatLng(newPos.lat, newPos.long))
                                .radius(DEFAULT_RAGGIO)
                        )
                        posAggiunte.add(newPos)
                        dialog.dismiss()
                    }
                }
            }
            dialog.show()

        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onMyLocationClick(loc: Location) {}


}