package com.example.masstouring.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.masstouring.R
import com.example.masstouring.common.LoggerTag
import com.example.masstouring.event.*
import com.example.masstouring.mapactivity.RecordItem
import com.example.masstouring.mapactivity.RecordState
import com.example.masstouring.recordservice.ILocationUpdateCallback
import com.example.masstouring.repository.RecordItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapActivtySharedViewModel @Inject constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository : RecordItemRepository
): ViewModel(), RecordItemRepository.IRecordItemListener{
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
    val fitAreaEvent = MutableLiveData<FitAreaEvent>()

    init {
        repository.addRecordItemStateChangeListener(this);
    }

    override fun onRecordItemStateChanged(recordItem: RecordItem) {
        //do nothing
    }

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

    fun isNothingRendered(): Boolean {
        return repository.getCachedRecordItems().stream().noneMatch(RecordItem::isRendered)
    }

    public interface IRecordItemOperationCallback{
        fun onCompleting()
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeRecordItemStateChangeListener(this)
        repository.getCachedRecordItems().stream().forEach {
            it -> it.initializeUIFlags()
        }
    }

    fun updateRecordItem(newRecordItem: RecordItem){
        repository.updateRecordItem(newRecordItem)
    }
}