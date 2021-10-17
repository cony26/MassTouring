package com.example.masstouring.mapactivity.presenter

import android.widget.Button
import androidx.lifecycle.LifecycleOwner
import com.example.masstouring.mapactivity.MapActivtySharedViewModel

class CheckRecordsButtonPresenter (private val button : Button, private val owner : LifecycleOwner, private val viewModel : MapActivtySharedViewModel) : IPresenter {
    init{
        owner.lifecycle.addObserver(this);
        button.setOnClickListener { viewModel.onCheckRecordsButtonClick() }
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
}