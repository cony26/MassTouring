package com.example.masstouring.viewmodel

import android.location.Location
import android.util.Log
import androidx.lifecycle.*
import com.example.masstouring.common.LoggerTag
import com.example.masstouring.mapactivity.PolylineInfo
import com.example.masstouring.mapactivity.RecordItem
import com.example.masstouring.repository.RecordItemRepository
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GoogleMapViewModel @Inject constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository : RecordItemRepository
): ViewModel(), RecordItemRepository.IRecordItemListener{
    var recordingPolyline: Polyline? = null
    var recordingPolylineOptions: PolylineOptions? = null
    val renderedPolylineInfo: MutableLiveData<MutableMap<Int, PolylineInfo>> = MutableLiveData(mutableMapOf())
    val isClusterDistributed = MutableLiveData(false)

    init {
        repository.addRecordItemStateChangeListener(this)
    }

    override fun onRecordItemStateChanged(recordItem: RecordItem) {
        viewModelScope.launch {

            val map = renderedPolylineInfo.value

            if(map == null){
                Log.e(LoggerTag.POLYLINE_PROCESS, "unexpected state. renderedPolylineInfo is null")
                return@launch
            }

            if(recordItem.isRendered){
                if(!map.containsKey(recordItem.id)){
                    val polylineOptionsList: List<PolylineOptions> = recordItem.createPolylineOptions()
                    map[recordItem.id] = PolylineInfo(polylineOptionsList)
                    renderedPolylineInfo.postValue(map)
                    Log.v(LoggerTag.POLYLINE_PROCESS, "add " + recordItem.id + "renderedPolylineInfo")
                }
            }else{
                if(map.containsKey(recordItem.id)){
                    map.remove(recordItem.id)
                    renderedPolylineInfo.postValue(map)
                    Log.v(LoggerTag.POLYLINE_PROCESS, "remove " + recordItem.id + "renderedPolylineInfo")
                }
            }
        }
    }

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

    override fun onCleared() {
        super.onCleared()
        repository.removeRecordItemStateChangeListener(this)
    }

    fun clearPolyline(){
        renderedPolylineInfo.value?.values?.stream()?.forEach { info -> info.polylineList = null }
    }
}