package com.example.masstouring.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masstouring.R
import com.example.masstouring.common.LoggerTag
import com.example.masstouring.mapactivity.*
import com.example.masstouring.recordservice.ILocationUpdateCallback
import com.example.masstouring.repository.Repository
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapActivtySharedViewModel @Inject constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository : Repository
): ViewModel() {
    val isTracePosition = MutableLiveData(true)
    val recordState = MutableLiveData(RecordState.STOP)
    val recordStartEvent = MutableLiveData<RecordStartEvent>()
    val recordEndEvent = MutableLiveData<RecordEndEvent>()
    val recordServiceOrderEvent = MutableLiveData<RecordServiceOrderEvent>()
    val isRecordServiceBound = MutableLiveData(false)
    val isRecordsViewVisible = MutableLiveData(false)
    val deleteRecordsIconVisible = MutableLiveData(false)
    val isClusterDistributed = MutableLiveData(false)
    val isRecording: Boolean
        get() = recordState.value == RecordState.RECORDING
    val locationUpdateCallback = MutableLiveData<ILocationUpdateCallback>()
    val restoreEvent = MutableLiveData<RestoreFromServiceEvent>()

    //Start/EndRecordButton
    fun onRecordButtonClick() {
        val state: RecordState? = recordState.value
        when (state) {
            RecordState.RECORDING -> {
                recordState.value = RecordState.STOP
                recordEndEvent.value = RecordEndEvent(R.string.touringFinishToast)
                recordServiceOrderEvent.value = RecordServiceOrderEvent(RecordServiceOrderEvent.Order.END)
            }
            RecordState.STOP -> {
                recordState.value = RecordState.RECORDING
                recordStartEvent.value = RecordStartEvent(R.string.touringStartToast)
            }
            else -> Log.e(LoggerTag.SYSTEM_PROCESS, "unexpected record state detected:" + state?.id)
        }
    }

    //CheckRecordsButton
    fun onCheckRecordsButtonClick(){
        isRecordsViewVisible.value?.let{
            isRecordsViewVisible.value = !it
        }
    }

    fun onDeletePositiveClick(boundRecordView: BoundRecordView){
        viewModelScope.launch {
            boundRecordView.deleteSelectedItems()
        }
        deleteRecordsIconVisible.value = false
    }

    fun getLastLatLngFrom(aId: Int): LatLng?{
        return repository.getLastLatLngFrom(aId)
    }

    fun getRecords(): List<RecordItem?>?{
        return repository.getRecords()
    }

    fun getRecordSize(): Int{
        return repository.getRecordSize()
    }

    fun deleteRecord(aIds: IntArray){
        repository.deleteRecord(aIds)
    }

}