package com.example.masstouring.mapactivity;

import android.content.Context;

import androidx.lifecycle.ViewModelProvider;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class RecordStartWorker extends Worker {
    public RecordStartWorker(Context aContext, WorkerParameters params){
        super(aContext, params);

    }

    @Override
    public Result doWork(){


        return Result.success();
    }
}
