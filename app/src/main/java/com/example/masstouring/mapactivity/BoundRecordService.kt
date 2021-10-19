package com.example.masstouring.mapactivity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import com.example.masstouring.common.LoggerTag
import com.example.masstouring.recordservice.RecordService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BoundRecordService(val viewModel: MapActivtySharedViewModel, val appCompatActivity: AppCompatActivity) : LifecycleObserver {
    private var recordService: RecordService? = null
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

                //Service : Recording, Activity : Recording not start
                //restore from service
                if(it.recordState.equals(RecordState.RECORDING)){
                    viewModel.recordState.value = RecordState.RECORDING
                    val recordId: Int? = recordService?.recordObject?.recordId
                    viewModel.restoreEvent.value = RestoreFromServiceEvent(recordId)
                }
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
        if (viewModel.isRecordServiceBound.value!!) {
            recordService?.setUnbindRequestCallback(null)
            recordService?.setIRecordServiceCallback(null)
            appCompatActivity.unbindService(oRecordServiceConnection)
            viewModel.isRecordServiceBound.value = false
            Log.d(LoggerTag.SYSTEM_PROCESS, "unbind RecordService")
        }
    }

    private fun stopServiceIfNotRecording() {
        if (viewModel.isRecording) return

        if (viewModel.isRecordServiceBound.value!!) {
            recordService?.stopService()
        }
    }

    private fun subscribeLiveData(){
        viewModel.recordServiceOrderEvent.observe(appCompatActivity, Observer {
            val order = it.contentIfNotHandled
            if(order.equals(RecordServiceOrderEvent.Order.START)){
                val i = Intent(appCompatActivity.applicationContext, RecordService::class.java)
                appCompatActivity.startForegroundService(i)
                appCompatActivity.bindService(i, oRecordServiceConnection, AppCompatActivity.BIND_AUTO_CREATE)
                GlobalScope.launch {
                    while(!viewModel.isRecordServiceBound.value!!){
                        delay(10)
                    }
                    recordService?.startRecording();
                }
            }else if(order.equals(RecordServiceOrderEvent.Order.END)){
                if (viewModel.isRecordServiceBound.value!!) {
                    recordService?.stopRecording()
                }
            }
        })

        viewModel.locationUpdateCallback.observe(appCompatActivity, Observer {
            recordService?.setIRecordServiceCallback(viewModel.locationUpdateCallback.value)
        })
    }
}