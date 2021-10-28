package com.example.masstouring.viewmodel

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.masstouring.repository.Repository
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GoogleMapViewModel @Inject constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository : Repository
): ViewModel(){
    var recordingLastPolyline: Polyline? = null
    var recordingPolylineOptions: PolylineOptions? = null

    fun updatePolyline(googleMap: GoogleMap, location: Location){
        recordingLastPolyline?.remove()

        recordingPolylineOptions?.let{
            it.add(LatLng(location.latitude, location.longitude))
            recordingLastPolyline = googleMap.addPolyline(it)
        }
    }

    fun restorePolylineOptionsFrom(googleMap: GoogleMap, aRecordId: Int){
        recordingPolylineOptions?.let {
            repository.restorePolylineOptionsFrom(aRecordId)
            recordingLastPolyline = googleMap.addPolyline(it)
        }
    }

}