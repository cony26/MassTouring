package com.example.masstouring.mapactivity;

import androidx.activity.OnBackPressedCallback;

/**
 * If priority is high, the callback is registered to {@link androidx.activity.OnBackPressedDispatcher} at last.
 * That is, the callback is called first when back pressed.
 */
public abstract class PrioritizedOnBackPressedCallback extends OnBackPressedCallback implements Comparable<PrioritizedOnBackPressedCallback> {
    private final int oPriority;
    public final static int RECORD_VIEW = 10;
    public final static int CLUSTER_DISTRIBUTED = 100;

    public PrioritizedOnBackPressedCallback(boolean aEnabled, int aPriority) {
        super(aEnabled);
        oPriority = aPriority;
    }

    public int getPriority() {
        return oPriority;
    }

    @Override
    public int compareTo(PrioritizedOnBackPressedCallback o) {
        int comparePriority = o.getPriority();
        if(oPriority < comparePriority){
            return -1;
        }else if(oPriority > comparePriority){
            return 1;
        }else{
            return 0;
        }
    }
}
