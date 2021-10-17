package com.example.masstouring.mapactivity.presenter

import android.widget.Button
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.masstouring.mapactivity.MapActivtySharedViewModel
import com.example.masstouring.mapactivity.RecordState

class RecordButtonPresenter(private val button : Button, private val owner : LifecycleOwner, private val viewModel : MapActivtySharedViewModel) : IPresenter {
    init{
        owner.lifecycle.addObserver(this);
        button.setOnClickListener { viewModel.onRecordButtonClick() }
        subscribeLiveData()
    }

    override fun onCreate() {
    }

    override fun onStart() {
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onStop() {
    }

    override fun onDestroy() {
    }

    private fun subscribeLiveData(){
        viewModel.recordState.observe(owner, Observer<RecordState> {
            recordState -> button.setText(recordState.buttonStringId)
        })
    }

}