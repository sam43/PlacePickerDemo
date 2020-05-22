package com.sam43.placepickerdemo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapFragment: SupportMapFragment
    private var hasDestinationPressed: Boolean = false
    private var isFirstTime: Boolean = true
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var mLastKnownLocation: Location? = null
    private var mLocationAddress: String? = null
    private var mLocationPermissionGranted = false
    private val mDefaultLocation = LatLng(23.7937704, 90.4130481)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        locationPermission
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupOnClicks

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        Log.d("mFusedLocation", "value: $mFusedLocationProviderClient")
    }

    private val setupOnClicks: Unit
        get() {
            etWhereTo.setOnClickListener {
                hasDestinationPressed = true
                onSearchCalled
            }

            etFromWhere.setOnClickListener {
                hasDestinationPressed = false
                onSearchCalled
            }
        }

    override fun onResume() {
        super.onResume()
        initializePlacesApi
        pickCurrentPlace()
    }

    /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */

    /**
     * Prompts the user for permission to use the device location.
     */
    private val locationPermission: Unit
        get() {
            /*
             * Request location permission, so that we can get the location of the
             * device. The result of the permission request is handled by a callback,
             * onRequestPermissionsResult.
             */
            mLocationPermissionGranted = false
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionGranted = true
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
        }

    /**
     * Get the current location of the device, and position the map's camera
     */
    private val deviceLocation: Unit
        get() {
            /*
             * Get the best and most recent location of the device, which may be null in rare
             * cases when a location is not available.
             */
            try {
                if (mLocationPermissionGranted) {
                    val locationResult = mFusedLocationProviderClient.lastLocation
                    locationResult.addOnSuccessListener(this) { location ->
                        Log.d("mFusedLocation1", "value: $location")
                        if (location != null) {
                            // Set the map's camera position to the current location of the device.
                            setLocationValue(location)

                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.")
                            mMap.moveCamera(
                                CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, 15f)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Exception: %s", e.message.toString())
            }
        }

    private fun setLocationValue(location: Location?) {
        // Update user current location
        if (isFirstTime) {
            val currentLatLng = location?.let { LatLng(it.latitude, it.longitude) }
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                currentLatLng, 15f
            )
            mMap.moveCamera(cameraUpdate)
            //mMap.animateCamera(cameraUpdate, 4000, null)
            val address = currentLatLng?.let {
                getLocationAddress(it.latitude, it.longitude)["address"]
            }
            etFromWhere.setText(address)
            isFirstTime = false
        } else {
            mLastKnownLocation = location
        }
    }

    /**
     * Fetch a list of likely places, and show the current place on the map - provided the user
     * has granted location permission.
     */
    private fun pickCurrentPlace() {
        if (mLocationPermissionGranted) {
            Log.i(TAG + "8888", "The user granted location permission.")
            deviceLocation
        } else {
            // The user has not granted permission.
            Log.i(TAG, "The user did not grant location permission.")

            // Add a default marker, because the user hasn't selected a place.
            mMap.addMarker(
                MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .snippet(getString(R.string.default_info_snippet))
            )

            // Prompt the user for permission.
            locationPermission
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        mLocationPermissionGranted = false
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) { // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionGranted = true
            }
        } else {
            Toast.makeText(this, "Please enable location permission", Toast.LENGTH_LONG).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // I did when I get a location to be updated
        // see "getDeviceLocation()" method for the details
        // Prompt the user for permission.
        mMap.uiSettings.isMapToolbarEnabled = true
        mMap.uiSettings.setAllGesturesEnabled(true)
        // Enable the zoom controls for the map
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        val locationButton = (mapFragment.view?.findViewById<View>("1".toInt())
            ?.parent as View).findViewById<View>("2".toInt())

        // and next place it, for exemple, on bottom right (as Google Maps app)

        // and next place it, for exemple, on bottom right (as Google Maps app)
        val rlp: RelativeLayout.LayoutParams =
            locationButton.layoutParams as RelativeLayout.LayoutParams
        // position on right bottom
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        rlp.setMargins(0, 0, 0, 300)
    }

    private fun updateMapLocation(place: Place) {
        if (hasDestinationPressed)
            etWhereTo.setText(place.address)
        else
            etFromWhere.setText(place.address)

        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
            place.latLng, 15f
        )
        mMap.addMarker(place.let {
            MarkerOptions().position(it.latLng!!).title(it.name).snippet(mLocationAddress)
        })

        mMap.animateCamera(cameraUpdate, 2000, null)
    }

    private val initializePlacesApi: Unit
        get() {
            /**
             * Initialize Places. For simplicity, the API key is hard-coded. In a production
             * environment we recommend using a secure mechanism to manage API keys.
             */
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, getString(R.string.api_key))
            }
        }

    private val onSearchCalled: Unit
        get() {
            // Set the fields to specify which types of place data to return.
            val fields =
                listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            // Start the autocomplete intent.
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields
            ).setCountry("BD") //Bangladesh
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    Log.i(TAG, "Place: " + place.name + ", " + place.latLng + ", " + place.address)
                    mLocationAddress = place.address
                    // TODO: Get info about the selected place.
                    updateMapLocation(place)

                }
                AutocompleteActivity.RESULT_ERROR -> {
                    // TODO: Handle the error.
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    Toast.makeText(this, "Error: " + status.statusMessage, Toast.LENGTH_LONG).show()
                    Log.i(TAG, status.statusMessage.toString())
                }
                Activity.RESULT_CANCELED -> {
                    // The user canceled the operation.
                    Log.wtf(TAG, "Something went wrong! [user cancelled the search]")
                }
            }
        }
    }


    companion object {
        private val TAG: String = MapsActivity::class.simpleName.toString()
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val AUTOCOMPLETE_REQUEST_CODE = 22
    }
}
