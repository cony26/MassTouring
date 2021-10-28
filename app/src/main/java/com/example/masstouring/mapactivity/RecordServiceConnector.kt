package com.example.masstouring.mapactivity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.example.masstouring.common.LoggerTag
import com.example.masstouring.event.RecordServiceOrderEvent
import com.example.masstouring.event.RestoreFromServiceEvent
import com.example.masstouring.recordservice.RecordService
import com.example.masstouring.viewmodel.MapActivtySharedViewModel
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@ActivityScoped
class RecordServiceConnector @Inject constructor(
        val appCompatActivity: FragmentActivity
        ) : LifecycleObserver {
    private var recordService: RecordService? = null
    private val viewModel : MapActivtySharedViewModel = ViewModelProvider(appCompatActivity).get(MapActivtySharedViewModel::class.java)

    init {
        subscribeLiveData()
        appCompatActivity.lifecycle.addObserver(this)
    }

    private val oRecordServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder = iBinder as RecordService.RecordServiceBinder
            recordService = binder.recordService
            viewModel.isRecordServiceBound.value = true
            recordService?.let{
                it.setUnbindRequestCallback(stopRequestCallback)
                it.setIRecordServiceCallback(viewModel.locationUpdateCallback.value)
            }
            Log.d(LoggerTag.SYSTEM_PROCESS, "onServiceConnected MapActivity")
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            viewModel.isRecordServiceBound.value = false
            Log.d(LoggerTag.SYSTEM_PROCESS, "onServiceDisconnected MapActivity")
        }
    }

    private val stopRequestCallback = RecordService.IStopRequestCallback {
        viewModel.recordState.value = RecordState.STOP
        unbindServiceGracefully()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause(){
        stopServiceIfNotRecording()
        unbindServiceGracefully()
    }

    private fun unbindServiceGracefully() {
        viewModel.isRecordServiceBound.value?.let {
            if (it) {
                recordService?.setUnbindRequestCallback(null)
                recordService?.setIRecordServiceCallback(null)
                appCompatActivity.unbindService(oRecordServiceConnection)
                viewModel.isRecordServiceBound.value = false
                Log.d(LoggerTag.SYSTEM_PROCESS, "unbind RecordService")
            }
        }
    }

    private fun stopServiceIfNotRecording() {
        if (viewModel.isRecording) return

        if (viewModel.isRecordServiceBound.value!!) {
            recordService?.stopService()
        }
    }

    private fun subscribeLiveData(){
        viewModel.recordServiceOrderEvent.observe(appCompatActivity, Observer { event ->
            val order = event.contentIfNotHandled
            when(order){
                RecordServiceOrderEvent.Order.START ->{
                    bindRecordService()
                    startRecording()
                }
                RecordServiceOrderEvent.Order.BOUND ->{
                    bindRecordService()
                    restoreFromService()
                }
                RecordServiceOrderEvent.Order.END ->{
                    viewModel.isRecordServiceBound.value?.let {
                        if (it) {
                            recordService?.stopRecording()
                        }
                    }
                }
            }
        })

        viewModel.locationUpdateCallback.observe(appCompatActivity, Observer {
            recordService?.setIRecordServiceCallback(viewModel.locationUpdateCallback.value)
        })
    }

    private fun bindRecordService(){
        val i = Intent(appCompatActivity.applicationContext, RecordService::class.java)
        appCompatActivity.startForegroundService(i)
        appCompatActivity.bindService(i, oRecordServiceConnection, AppCompatActivity.BIND_AUTO_CREATE)
    }

    private fun restoreFromService(){
        recordService?.let {
            //Service : RECORDING, Activity : STOP
            //restore from service
            if(it.recordState.equals(RecordState.RECORDING)){
                viewModel.recordState.value = RecordState.RECORDING
                val recordId: Int? = it.recordObject?.recordId
                viewModel.restoreEvent.value = RestoreFromServiceEvent(recordId)
            }
            //Service : STOP, Activity : STOP
            else{
                viewModel.recordState.value = RecordState.STOP
            }
        }
    }

    private fun startRecording(){
        GlobalScope.launch {
            viewModel.isRecordServiceBound.value?.let {
                while(!it){
                    delay(10)
                }
                recordService?.startRecording();
            }
        }
    }
}