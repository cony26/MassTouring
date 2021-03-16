package com.example.masstouring.mapactivity;

import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.example.masstouring.common.LoggerTag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BackPressedCallbackRegisterer implements LifecycleObserver {
    private final AppCompatActivity oAppCompatActivity;
    private List<PrioritizedOnBackPressedCallback> oOnBackPressedCallbackList = new ArrayList<>();
    private static BackPressedCallbackRegisterer oRegisterer = null;

    private BackPressedCallbackRegisterer(AppCompatActivity aAppCompatActivity){
        aAppCompatActivity.getLifecycle().addObserver(this);
        oAppCompatActivity = aAppCompatActivity;
    }

    /**
     * @return instance of {@link BackPressedCallbackRegisterer} may be null
     */
    public static BackPressedCallbackRegisterer getInstance() {
        if(oRegisterer == null){
            Log.e(LoggerTag.SYSTEM_PROCESS, "BackPressedCallbackRegisterer is callled before initialization.");
        }
        return oRegisterer;
    }

    public static void initialize(AppCompatActivity aAppCompatActivity){
        oRegisterer = new BackPressedCallbackRegisterer(aAppCompatActivity);
    }

    public void register(PrioritizedOnBackPressedCallback aOnBackPressedCallback){
        oOnBackPressedCallbackList.add(aOnBackPressedCallback);
        update();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private static void clear(){
        oRegisterer = null;
    }

    private void update(){
        List<PrioritizedOnBackPressedCallback> sortedList = oOnBackPressedCallbackList.stream()
                .sorted((a,b) -> a.compareTo(b))
                .collect(Collectors.toList());

        int sortedNumber = checkSortedNumber(sortedList);
        int listSize = oOnBackPressedCallbackList.size();

        OnBackPressedDispatcher dispatcher = oAppCompatActivity.getOnBackPressedDispatcher();

        //For performance, the removed/added callbacks are limited as possible.
        if(sortedNumber == listSize){
            dispatcher.addCallback(oOnBackPressedCallbackList.get(listSize - 1));
        }else{
            oOnBackPressedCallbackList.stream()
                    .skip(sortedNumber)
                    .forEach(OnBackPressedCallback::remove);

            sortedList.stream()
                    .skip(sortedNumber)
                    .forEach(dispatcher::addCallback);

            oOnBackPressedCallbackList = sortedList;
        }
    }

    private int checkSortedNumber(List<PrioritizedOnBackPressedCallback> aSortedList){
        int sortedNumber = 0;

        for(int i = 0; i < aSortedList.size(); i++){
            if(aSortedList.get(i) != oOnBackPressedCallbackList.get(i)){
                return sortedNumber;
            }
            sortedNumber++;
        }

        return sortedNumber;
    }
}