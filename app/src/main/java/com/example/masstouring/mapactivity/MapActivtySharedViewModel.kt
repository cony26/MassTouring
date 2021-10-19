package com.example.masstouring.mapactivity

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.masstouring.R
import com.example.masstouring.common.LoggerTag
import com.example.masstouring.recordservice.ILocationUpdateCallback

class MapActivtySharedViewModel : ViewModel() {
    val isTracePosition = MutableLiveData(true)
    val recordState = MutableLiveData(RecordState.STOP)
    val recordStartEvent = MutableLiveData<RecordStartEvent>()
    val recordEndEvent = MutableLiveData<RecordEndEvent>()
    val recordServiceOrderEvent = MutableLiveData<RecordServiceOrderEvent>()
    val isRecordServiceBound = MutableLiveData(false)
    val isRecordsViewVisible = MutableLiveData(false)
    val toolbarVisibility = MutableLiveData(View.GONE)
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
        if (isRecordsViewVisible.value!!) {
            isRecordsViewVisible.value = false
            toolbarVisibility.value = View.GONE
        } else {
            isRecordsViewVisible.value = true
        }
    }
}