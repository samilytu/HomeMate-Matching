package com.samilozturk.homemate_matching.ui.home.students.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import com.samilozturk.homemate_matching.R
import com.samilozturk.homemate_matching.data.model.Student
import com.samilozturk.homemate_matching.data.source.db.DbRepository
import com.samilozturk.homemate_matching.databinding.ActivityMapBinding
import com.samilozturk.homemate_matching.ui.home.profile.ProfileActivity
import com.samilozturk.homemate_matching.util.icon
import com.samilozturk.homemate_matching.util.snackbar
import com.samilozturk.homemate_matching.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MapActivity : AppCompatActivity() {

    @Inject
    lateinit var dbRepository: DbRepository
    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap

    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val locationRequest by lazy {
        LocationRequest.Builder(1000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
    }

    private var markerMyLocation: Marker? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                Log.d(
                    "MapFragment",
                    "onRequestPermissionsResult:  Precise location access granted."
                )
                startLocationUpdates()
            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                Log.d(
                    "MapFragment",
                    "onRequestPermissionsResult:  Only approximate location access granted."
                )
                startLocationUpdates()
            }

            else -> {
                // No location access granted.
                Log.d("MapFragment", "onRequestPermissionsResult:  No location access granted.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get users from intent
        val students = intent.getParcelableArrayListExtra<Student>("students")!!

        binding.fabMyLocation.setOnClickListener {
            zoomToMyLocation()
        }

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            Log.d("MapFragment", "onViewCreated: checkPermissions true")

            val locationManager = getSystemService<LocationManager>()!!
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.d("MapFragment", "onViewCreated: GPS is Enabled in your device")
                startLocationUpdates()
            } else {
                Log.d("MapFragment", "onViewCreated: GPS is Disabled in your device")
                requestLocation()
            }

        }

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map_container) as SupportMapFragment

        lifecycleScope.launch {
            googleMap = mapFragment.awaitMap()

            try {
                students
                    .filter { it.homeAddress != null }
                    .forEach {
                        val marker = googleMap.addMarker {
                            position(it.homeAddress!!.location)
                            title(it.fullName)
                            snippet(it.homeAddress!!.address)
                            icon(this@MapActivity, R.drawable.ic_home)
                        }!!
                        marker.tag = it
                    }
            } catch (e: Exception) {
                snackbar(e.message.toString(), isError = true)
            }

            googleMap.setOnInfoWindowClickListener { marker ->
                val student = marker.tag as? Student
                    ?: return@setOnInfoWindowClickListener
                val intent = Intent(this@MapActivity, ProfileActivity::class.java)
                intent.putExtra("student", student)
                startActivity(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    private fun checkPermissions(): Boolean =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() =
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

    private fun requestLocation() {
        lifecycleScope.launch {
            try {
                val builder = LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)
                    .setAlwaysShow(true)
                    .build()

                val response = LocationServices.getSettingsClient(this@MapActivity)
                    .checkLocationSettings(builder).await()

                if (response.locationSettingsStates?.isLocationPresent == true) {
                    // Location settings are satisfied. Start location updates.
                    startLocationUpdates()
                } else {
                    toast("Konum ayarlarından konumunuzu açınız.")
                }
            } catch (e: ResolvableApiException) {
                e.printStackTrace()

                // Location settings are not satisfied. But could be fixed by showing the
                // user a dialog.
                try {
                    // Cast to a resolvable exception.
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this@MapActivity,
                        REQUEST_CODE_LOCATION_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    toast("Bilinmeyen bir hata oluştu.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                toast("Bilinmeyen bir hata oluştu.")
            }
        }

    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (checkPermissions()) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            requestPermissions()
        }
    }

    fun onLocationRequestResult() {
        startLocationUpdates()
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            locationResult.locations[0]?.let {
                Log.d("MapFragment", "onLocationResult: ${it.latitude}, ${it.longitude}")
                val locationInfo = LatLng(it.latitude, it.longitude)

                val geocoder = Geocoder(this@MapActivity, Locale.getDefault())

                val addresses = geocoder.getFromLocation(
                    locationInfo.latitude,
                    locationInfo.longitude,
                    1
                )

                addresses?.first()?.let { address ->
                    // generate address string in format "Mahalle, Sokak, No, Posta Kodu, İlçe, İl"
                    // consider null values
                    val addressString = listOfNotNull(
                        address.subLocality ?: address.locality,
                        address.thoroughfare,
                        address.subThoroughfare,
                        address.postalCode,
                        address.subAdminArea,
                        address.adminArea
                    ).joinToString(", ")
                    Log.d("MapFragment", "onLocationResult: $addressString")
                }


                if (markerMyLocation == null) {
                    markerMyLocation = googleMap.addMarker {
                        title("Konumum")
                        position(locationInfo)
                        icon(this@MapActivity, R.drawable.ic_person_pin)
                    }
                } else {
                    markerMyLocation!!.position = locationInfo
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun zoomToMyLocation() {
        if (markerMyLocation != null) {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    markerMyLocation!!.position,
                    15f
                )
            )
        } else {
            toast("Konumunuz alınamadı.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_LOCATION_PERMISSION -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("SelectLocationActivity", "onActivityResult: RESULT_OK")
                    startLocationUpdates()
                } else {
                    Log.d("SelectLocationActivity", "onActivityResult: RESULT_CANCELED")
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val REQUEST_CODE_LOCATION_PERMISSION = 1
    }
}
