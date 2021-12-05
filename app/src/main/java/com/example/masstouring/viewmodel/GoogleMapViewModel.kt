package com.example.masstouring.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
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
    var recordingPolyline: Polyline? = null
    var recordingPolylineOptions: PolylineOptions? = null
    var renderedPolylineMap: Map<Int, List<Polyline>> = HashMap()
    val isClusterDistributed = MutableLiveData(false)

    fun updatePolyline(googleMap: GoogleMap, location: Location){
        recordingPolyline?.remove()

        recordingPolylineOptions?.let{
            it.add(LatLng(location.latitude, location.longitude))
            recordingPolyline = googleMap.addPolyline(it)
        }
    }

    fun restorePolylineOptionsFrom(googleMap: GoogleMap, aRecordId: Int){
        recordingPolylineOptions?.let {
            repository.restorePolylineOptionsFrom(aRecordId)
            recordingPolyline = googleMap.addPolyline(it)
        }
    }

    fun getLastLatLngFrom(aId: Int): LatLng?{
        return repository.getLastLatLngFrom(aId)
    }

}