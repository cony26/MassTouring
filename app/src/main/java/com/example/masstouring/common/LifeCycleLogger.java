package com.example.masstouring.common;

import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

public class LifeCycleLogger implements LifecycleObserver {
    private final String oClassname;
    public LifeCycleLogger(LifecycleOwner aOwner, String aClassName){
        aOwner.getLifecycle().addObserver(this);
        oClassname = aClassName;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    void logOnCreate(){
        Log.d(LoggerTag.SYSTEM_PROCESS, "ON_CREATE:" + oClassname);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void logOnStart(){
        Log.d(LoggerTag.SYSTEM_PROCESS, "ON_START:" + oClassname);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void logOnResume(){
        Log.d(LoggerTag.SYSTEM_PROCESS, "ON_RESUME:" + oClassname);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    void logOnPause(){
        Log.d(LoggerTag.SYSTEM_PROCESS, "ON_PAUSE:" + oClassname);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void logOnStop(){
        Log.d(LoggerTag.SYSTEM_PROCESS, "ON_STOP:" + oClassname);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void logOnDestroy(){
        Log.d(LoggerTag.SYSTEM_PROCESS, "ON_DESTROY:" + oClassname);
    }
}
