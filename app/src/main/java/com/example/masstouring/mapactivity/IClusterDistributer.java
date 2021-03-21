package com.example.masstouring.mapactivity;

import android.view.MotionEvent;

import java.util.List;

public interface IClusterDistributer {
    List<ClusterDistributedDrawable> getClusterDistributedDrawableList();
    FocusedItem getFocusedItem();
    boolean onActionDown(MotionEvent event);
    boolean onActionUp(MotionEvent event);
}
