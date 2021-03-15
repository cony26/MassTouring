package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.google.maps.android.clustering.Cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ClusterDistributedView extends SurfaceView {
    private final List<ClusterDistributedDrawable> oClusterDistributedDrawableList = new ArrayList<>();
    private DistributedItem oTouchedItem = null;
    private FocusedItem oFocusedItem = null;
    private ClusterDistributedDrawTask oDrawTask;
    private final PrioritizedOnBackPressedCallback oOnBackPressedWhenFocused = new PrioritizedOnBackPressedCallback(false, PrioritizedOnBackPressedCallback.CLUSTER_DISTRIBUTED_ITEM_FOCUSED) {
        @Override
        public void handleOnBackPressed() {
            oFocusedItem.setEnable(false);
            oOnBackPressedWhenFocused.setEnabled(false);
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
                oDrawTask = new ClusterDistributedDrawTask(surfaceHolder, ClusterDistributedView.this);
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

    public boolean removeClusterDistributedDrawable(Cluster aCluster){
        List<ClusterDistributedDrawable> deleteList = new ArrayList<>();

        for(ClusterDistributedDrawable drawable: oClusterDistributedDrawableList){
            if(drawable.getCluster().equals(aCluster)){
                deleteList.add(drawable);
            }
        }

        if(deleteList.isEmpty()){
            return false;
        }else{
            deleteList.stream().forEach(oClusterDistributedDrawableList::remove);
            return true;
        }
    }

    public void clearClusterDistributedDrawable(){
        oClusterDistributedDrawableList.clear();
    }

    public void requestShutdownDrawingTask(){
        oDrawTask.requestShutDown();
        oDrawTask.interrupt();
    }

    public List<ClusterDistributedDrawable> getClusterDistributedDrawableList(){
        return oClusterDistributedDrawableList;
    }

    public boolean hasClusterDistributedDrawable(Cluster<Picture> aCluster){
        return oClusterDistributedDrawableList.stream()
                .anyMatch(drawable -> drawable.getCluster().equals(aCluster));
    }

    FocusedItem getFocusedItem(){
        return oFocusedItem;
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
        private boolean oShutdownRequested = false;
        private final SurfaceHolder oSurfaceHolder;
        private final ClusterDistributedView oClusterDistributedView;
        private final Paint p = new Paint();
        ClusterDistributedDrawTask(SurfaceHolder aSurfaceHolder, ClusterDistributedView aClusterDistributedView){
            oSurfaceHolder = aSurfaceHolder;
            oClusterDistributedView = aClusterDistributedView;
        }

        @Override
        public void run() {
            try{
                doWork(oSurfaceHolder);
            }catch(InterruptedException e){
                Log.d(LoggerTag.CLUSTER, "interrupted. shutdown ClusterDistributedView.");
            }finally {
                Log.i(LoggerTag.CLUSTER, "ClusterDistributedDrawTask is shutdown");
                //detach this view.
            }

        }

        private void doWork(SurfaceHolder aSurfaceHolder) throws InterruptedException{
            while(!isShutdownRequested()){
                Canvas canvas = lockAndGetCanvas(aSurfaceHolder);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                for(ClusterDistributedDrawable drawable: oClusterDistributedView.getClusterDistributedDrawableList()){
                    drawable.draw(canvas);
                }

                paintFocusedItem(canvas);

                aSurfaceHolder.unlockCanvasAndPost(canvas);
                waitForFpsTime();
            }
        }

        private Canvas lockAndGetCanvas(SurfaceHolder aSurfaceHolder){
            Canvas canvas = aSurfaceHolder.lockCanvas();
            while(canvas == null){
                Log.i(LoggerTag.CLUSTER, "can't lock canvas. wait getting lock...");
                try{
                    Thread.sleep(Const.FPS_MILLIS);
                }catch(InterruptedException e){
                    Log.e(LoggerTag.CLUSTER, "InterruptedException:", e);
                }
                canvas = aSurfaceHolder.lockCanvas();
            }

            return canvas;
        }

        private void paintFocusedItem(Canvas aCanvas){
            FocusedItem item = oClusterDistributedView.getFocusedItem();
            if(item.isEnabled()){
                aCanvas.drawBitmap(item.getBitmap(), null, item.getFocusedWindowRect(), p);
            }
        }

        private void waitForFpsTime(){
            try{
                Thread.sleep(Const.FPS_MILLIS);
            }catch(InterruptedException e){
                Log.e(LoggerTag.CLUSTER, "InterruptedException:", e);
            }
        }

        private boolean isShutdownRequested(){
            return oShutdownRequested;
        }

        public void requestShutDown(){
            oShutdownRequested = true;
        }
    }
}
