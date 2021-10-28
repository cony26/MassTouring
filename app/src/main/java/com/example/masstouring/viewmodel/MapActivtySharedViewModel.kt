package com.example.masstouring.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.masstouring.R
import com.example.masstouring.common.LoggerTag
import com.example.masstouring.event.*
import com.example.masstouring.mapactivity.RecordItem
import com.example.masstouring.mapactivity.RecordState
import com.example.masstouring.mapactivity.RecordViewController
import com.example.masstouring.recordservice.ILocationUpdateCallback
import com.example.masstouring.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
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
    val isRecording: Boolean
        get() = recordState.value == RecordState.RECORDING
    val locationUpdateCallback = MutableLiveData<ILocationUpdateCallback>()
    val restoreEvent = MutableLiveData<RestoreFromServiceEvent>()
    val polylineRenderEvent = MutableLiveData<PolylineRenderEvent>()
    val fitAreaEvent = MutableLiveData<FitAreaEvent>()
    val removeRecordItemEvent = MutableLiveData<RemoveRecordItemEvent>()
    val renderedIdList: List<Int> = ArrayList()

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

    fun onDeletePositiveClick(recordViewController: RecordViewController){
        viewModelScope.launch {
            recordViewController.deleteSelectedItems()
        }
        deleteRecordsIconVisible.value = false
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

    fun isRendered(aId: Int): Boolean {
        return renderedIdList.contains(aId)
    }

    fun isNothingRendered(): Boolean {
        return renderedIdList.isEmpty()
    }
}