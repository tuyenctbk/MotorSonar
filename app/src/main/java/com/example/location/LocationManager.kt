package com.example.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()
        } catch (e: Exception) {
            try {
                fusedLocationClient.lastLocation.await()
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun openNearbyMechanicsInMaps(lat: Double? = null, lng: Double? = null, query: String = "auto repair shop mechanic") {
        val uriStr = if (lat != null && lng != null) {
            "geo:$lat,$lng?q=${Uri.encode(query)}"
        } else {
            "geo:0,0?q=${Uri.encode(query)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback to browser Google Maps
            val webUri = if (lat != null && lng != null) {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng&query_place_id=$query")
            } else {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
            }
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
