package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.google.maps.android.clustering.Cluster;

import java.util.ArrayList;
import java.util.List;

public class ClusterDistributedView extends SurfaceView {
    private final List<ClusterDistributedDrawable> oClusterDistributedDrawableList = new ArrayList<>();
    private DistributedItem oTouchedItem = null;
    private FocusedItem oFocusedItem = null;
    private ClusterDistributedDrawTask oDrawTask = null;
    private final PrioritizedOnBackPressedCallback oOnBackPressedWhenFocused = new PrioritizedOnBackPressedCallback(false, PrioritizedOnBackPressedCallback.CLUSTER_DISTRIBUTED_ITEM_FOCUSED) {
        @Override
        public void handleOnBackPressed() {
            oFocusedItem.setEnable(false);
            oOnBackPressedWhenFocused.setEnabled(false);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                getWindowInsetsController().show(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                setSystemUiVisibility(View.VISIBLE);
            }
            Log.d(LoggerTag.SYSTEM_PROCESS,"back pressed when distributed item is focused");
        }
    };

    public ClusterDistributedView(Context context) {
        super(context);
        init();
    }

    public ClusterDistributedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClusterDistributedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                oDrawTask = new ClusterDistributedDrawTask(surfaceHolder, oClusterDistributedDrawableList, oFocusedItem);
                MapActivity.cExecutors.execute(oDrawTask);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);
        BackPressedCallbackRegisterer.getInstance().register(oOnBackPressedWhenFocused);
    }


    public void addClusterDistributedDrawable(ClusterDistributedDrawable aClusterDistributedDrawable){
        oClusterDistributedDrawableList.add(aClusterDistributedDrawable);
    }

    public boolean removeUnnecessaryClusterDistributedDrawable(){
        List<ClusterDistributedDrawable> deleteList = new ArrayList<>();

        for(ClusterDistributedDrawable drawable: oClusterDistributedDrawableList){
            if(drawable.getDistributedItems().size() == 0){
                deleteList.add(drawable);
            }
        }

        return removeClusterDistributedDrawable(deleteList);
    }

    public boolean removeClusterDistributedDrawable(Cluster aCluster){
        List<ClusterDistributedDrawable> deleteList = new ArrayList<>();

        for(ClusterDistributedDrawable drawable: oClusterDistributedDrawableList){
            if(drawable.getCluster().equals(aCluster)){
                deleteList.add(drawable);
            }
        }

        return removeClusterDistributedDrawable(deleteList);
    }

    private boolean removeClusterDistributedDrawable(List<ClusterDistributedDrawable> aDeletedList){
        if(aDeletedList.isEmpty()){
            return false;
        }

        for(ClusterDistributedDrawable drawable : aDeletedList){
            oClusterDistributedDrawableList.remove(drawable);
            Log.i(LoggerTag.CLUSTER, "removed ClusterDistributedDrawable");
        }
        return true;
    }

    public boolean removeDistributedItems(List<DistributedItem> aDistributedItems){
        boolean result = false;

        for(ClusterDistributedDrawable drawable : oClusterDistributedDrawableList){
            if(drawable.getDistributedItems().removeAll(aDistributedItems)){
                result = true;
            }
        }

        return result;
    }

    public void clearClusterDistributedDrawable(){
        oClusterDistributedDrawableList.clear();
    }

    public void requestShutdownDrawingTask(){
        if(oDrawTask != null){
            oDrawTask.requestShutDown();
        }
    }

    public List<ClusterDistributedDrawable> getClusterDistributedDrawableList(){
        return oClusterDistributedDrawableList;
    }

    public boolean hasClusterDistributedDrawable(Cluster<Picture> aCluster){
        return oClusterDistributedDrawableList.stream()
                .anyMatch(drawable -> drawable.getCluster().equals(aCluster));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                return distributedItemIsTouched((int)event.getX(), (int)event.getY());
            case MotionEvent.ACTION_UP:
                performClick();
                if(clickedItemIsSameWithTouchedItem((int)event.getX(), (int)event.getY())){
                    oFocusedItem.update(oTouchedItem, getContext());
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                        getWindowInsetsController().hide(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                    }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                        setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    }
                    oOnBackPressedWhenFocused.setEnabled(true);
                }
                oTouchedItem = null;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private boolean distributedItemIsTouched(int aX, int aY){
        oTouchedItem = null;

        for(ClusterDistributedDrawable drawable : oClusterDistributedDrawableList){
            DistributedItem item = drawable.findDistributedItem(aX, aY);
            if(item != null){
                oTouchedItem = item;
                break;
            }
        }

        return oTouchedItem != null;
    }

    private boolean clickedItemIsSameWithTouchedItem(int aX, int aY) {
        if (oTouchedItem.getRect().contains(aX, aY)) {
            Log.i(LoggerTag.CLUSTER, "Distributed View is Touched:" + oTouchedItem.toString());
            return true;
        }

        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        ViewGroup parent = (ViewGroup)getParent();
        oFocusedItem = new FocusedItem(parent.getMeasuredWidth(), parent.getMeasuredHeight());
        super.onAttachedToWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if(hasWindowFocus){
        }else{
            requestShutdownDrawingTask();
        }
        super.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    protected void onDetachedFromWindow() {
        requestShutdownDrawingTask();
        super.onDetachedFromWindow();
    }

    private static class ClusterDistributedDrawTask extends Thread{
        private volatile boolean oShutdownRequested = false;
        private final SurfaceHolder oSurfaceHolder;
        private final Paint p = new Paint();
        private final FocusedItem oFocusedItem;
        private final List<ClusterDistributedDrawable> oClusterDistributedDrawableList;
        ClusterDistributedDrawTask(SurfaceHolder aSurfaceHolder, List<ClusterDistributedDrawable> aClusterDistributedDrawableList, FocusedItem aFocusedItem){
            oSurfaceHolder = aSurfaceHolder;
            oClusterDistributedDrawableList = aClusterDistributedDrawableList;
            oFocusedItem = aFocusedItem;
        }

        @Override
        public void run() {
            Log.d(LoggerTag.CLUSTER, "ClusterDistributedDrawTask is started");
            try{
                while(!oShutdownRequested){
                    doWork();
                }
            }catch(InterruptedException e){
                Log.d(LoggerTag.CLUSTER, "interrupted. shutdown ClusterDistributedView.");
            }finally {
                Log.i(LoggerTag.CLUSTER, "ClusterDistributedDrawTask is shutdown");
            }

        }

        private void doWork() throws InterruptedException{
            Canvas canvas = lockAndGetCanvas(oSurfaceHolder);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            for(ClusterDistributedDrawable drawable: oClusterDistributedDrawableList){
                drawable.draw(canvas);
            }

            paintFocusedItem(canvas);

            oSurfaceHolder.unlockCanvasAndPost(canvas);

            Thread.sleep(Const.FPS_MILLIS);
        }

        private Canvas lockAndGetCanvas(SurfaceHolder aSurfaceHolder) throws InterruptedException{
            Canvas canvas = aSurfaceHolder.lockCanvas();
            while(canvas == null){
                Log.i(LoggerTag.CLUSTER, "can't lock canvas. wait getting lock...");
                Thread.sleep(Const.FPS_MILLIS);
                canvas = aSurfaceHolder.lockCanvas();
            }

            return canvas;
        }

        private void paintFocusedItem(Canvas aCanvas){
            if(oFocusedItem == null){
                return;
            }

            if(oFocusedItem.isEnabled()){
                aCanvas.drawColor(Color.BLACK);
                aCanvas.drawBitmap(oFocusedItem.getBitmap(), null, oFocusedItem.getFocusedWindowRect(), p);
            }
        }

        public void requestShutDown(){
            oShutdownRequested = true;
            interrupt();
        }
    }
}
