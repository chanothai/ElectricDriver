package com.ballomo.electricdriver

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import io.nlopez.smartlocation.OnLocationUpdatedListener
import io.nlopez.smartlocation.SmartLocation
import io.nlopez.smartlocation.location.config.LocationAccuracy
import io.nlopez.smartlocation.location.config.LocationParams
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import kotlin.coroutines.CoroutineContext

private const val REQUEST_ACCESS_FINE_LOCATION = 0
@RuntimePermissions
class MainActivity : AppCompatActivity(), CoroutineScope, OnMapReadyCallback,
    OnLocationUpdatedListener {

    private var mMap: GoogleMap? = null

    private val job = Job()

    private var firestore = FirebaseFirestore.getInstance()

    private val user by lazy {
        return@lazy User(
            "ada",
            "Lovelace",
            1815,
            "0949948249"
        )
    }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment =
            supportFragmentManager.findFragmentById(mapDriver.id) as SupportMapFragment
        mapFragment.apply {
            getMapAsync(this@MainActivity)
        }

        showLocation.text = "Test"

        launch {
            addUser()
        }
    }

    private suspend fun addUser() {
        withContext(Dispatchers.IO) {
            Tasks.await(firestore.collection("users").document(user.phone).set(user))
        }
    }

    private suspend fun getUser(): List<User> {
        val users = mutableListOf<User>()
        val response = withContext(Dispatchers.IO) {
            Tasks.await(firestore.collection("users").get())
        }

        response.let {
            for (document in it) {
                val user = document.toObject(User::class.java)
                users.add(user)
            }
        }

        return users
    }

    private suspend fun deleteUser(user: User) {
        withContext(Dispatchers.IO) {
            Tasks.await(firestore.collection("users").document(user.phone).delete())
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let {
            mMap = it
        }
    }


    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun getCurrentLocation() {
        setupLocationProvider()
    }


    private fun setupLocationProvider() {
        val params = LocationParams.Builder()
            .setAccuracy(LocationAccuracy.HIGH)
            .setInterval(5000)
            .build()

        if (SmartLocation.with(this).location().state().locationServicesEnabled()) {
            SmartLocation.with(this)
                .location()
                .config(params)
                .start(this)
        } else {
            Toast.makeText(this, "Location Service Not Enable", Toast.LENGTH_LONG).show()
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onLocationUpdated(location: Location?) {
        location?.let {
            val latitude = it.latitude
            val longitude = it.longitude
            val accuracy = it.accuracy
            val bearing = it.bearing
            val provider = it.provider

            showLocation.text = "Location Update -> $latitude, $longitude, $accuracy, $bearing, $provider"
            Log.d("Location", "Location Update -> $latitude, $longitude, $accuracy, $bearing, $provider")
            return@let
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when(requestCode) {
            REQUEST_ACCESS_FINE_LOCATION -> {
                setupLocationProvider()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        getCurrentLocationWithPermissionCheck()
    }

    override fun onStop() {
        super.onStop()
        SmartLocation.with(this)
            .location()
            .stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
