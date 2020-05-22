package com.sam43.placepickerdemo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.widget.Toast
import java.io.IOException
import java.util.*


fun Context.getLocationAddress(latitude: Double, longitude: Double): HashMap<String, String> {
    val addresses: List<Address>
    val geocoder = Geocoder(this, Locale.getDefault())
    val addressMap = HashMap<String, String>()
    try {
        addresses = geocoder.getFromLocation(
            latitude,
            longitude,
            1
        ) // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        val address =
            addresses[0].getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        val city = addresses[0].locality
        val state = addresses[0].adminArea
        val country = addresses[0].countryName
        val postalCode = addresses[0].postalCode
        val knownName = addresses[0].featureName // Only if available else return NULL
        addressMap["address"] = address
        addressMap["city"] = city
        addressMap["state"] = state
        addressMap["postalCode"] = postalCode
        addressMap["country"] = country
        addressMap["knownName"] = knownName
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return addressMap
}

fun Context.toast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()