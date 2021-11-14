package com.example.masstouring.common;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ApplicationLifeCycleObserver implements LifecycleObserver {
    private final Context oContext;
    private int oProcNumber = 0;
    private LoggerTask oLoggerTask;

    @Inject
    ApplicationLifeCycleObserver(@ApplicationContext Context aContext){
        oContext = aContext;
    }

    public void register(LifecycleOwner aOwner){
        aOwner.getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    synchronized void addProcess(){
        oProcNumber++;
        if(oLoggerTask == null || oLoggerTask.isLoggingCompleted()){
            oLoggerTask = new LoggerTask(oContext, this);
            oLoggerTask.start();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    synchronized void deleteProcess(){
        oProcNumber--;
    }

    boolean isAnyProcessRunning(){
        return oProcNumber > 0;
    }
}
