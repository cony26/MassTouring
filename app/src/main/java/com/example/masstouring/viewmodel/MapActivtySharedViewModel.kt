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

    fun onDeletePositiveClick(callback: IRecordItemOperationCallback){
        viewModelScope.launch {
            deleteRecord(getSelectedItemIdList().toIntArray())
            callback.onCompleting()
        }
        deleteRecordsIconVisible.value = false
    }

    /**
     * get RecordItem List.<br>
     * if {@code force} is true, reload the all RecordItem from Database. It may take time.<br>
     * if {@code force} is false, get RecordItem List from cached data.
     */
    fun getRecordItems(force : Boolean): List<RecordItem>{
        if(force){
            return repository.cleanLoadRecordItems()
        }else{
            return repository.getCachedRecordItems()
        }
    }

    /**
     * get RecordItem of {@code aId}.<br>
     * If the record is already registered as cache, this immediately returns the value.<br>
     * Otherwise, this takes a few times to load data from DB.
     *
     * @see loadRecordAsync for async process
     */
    fun getRecord(aId : Int): RecordItem{
        return repository.loadRecordItem(aId)
    }

    fun loadRecordAsync(aId : Int, aCallback : IRecordItemOperationCallback){
        viewModelScope.launch {
            repository.loadRecordItem(aId)
            aCallback.onCompleting()
        }
    }

    fun getSelectedItemIdList(): List<Int> {
        return getRecordItems(false)
                .filter { it.isSelected }
                .map{ it.id }
                .toList()
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

    public interface IRecordItemOperationCallback{
        fun onCompleting()
    }
}