package com.example.masstouring.mapactivity;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class BackPressedCallbackRegister {
    private final AppCompatActivity oAppCompatActivity;
    private final List<PrioritizedOnBackPressedCallback> oOnBackPressedCallbackList = new ArrayList<>();
    BackPressedCallbackRegister(AppCompatActivity aAppCompatActivity){
        oAppCompatActivity = aAppCompatActivity;
    }

    public void register(PrioritizedOnBackPressedCallback aOnBackPressedCallback){
        oOnBackPressedCallbackList.add(aOnBackPressedCallback);
        update();
    }

    private void update(){
        oOnBackPressedCallbackList.stream().forEach(callback -> callback.remove());

        oOnBackPressedCallbackList.stream()
                .sorted((a,b) -> a.compareTo(b))
                .forEach(callback -> oAppCompatActivity.getOnBackPressedDispatcher().addCallback(callback));
    }
}
