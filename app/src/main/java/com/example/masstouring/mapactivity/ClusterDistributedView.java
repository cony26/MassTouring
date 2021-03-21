package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;

import java.util.List;

public class ClusterDistributedView extends SurfaceView {
    private IClusterDistributer oClusterDistributer;
    private ClusterDistributedDrawTask oDrawTask = null;

    public ClusterDistributedView(Context context, IClusterDistributer aClusterDistributer) {
        super(context);
        oClusterDistributer = aClusterDistributer;
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
                oDrawTask = new ClusterDistributedDrawTask(surfaceHolder, oClusterDistributer.getClusterDistributedDrawableList(), oClusterDistributer.getFocusedDrawable());
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
    }

    public void requestShutdownDrawingTask(){
        if(oDrawTask != null){
            oDrawTask.requestShutDown();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        Log.d(LoggerTag.CLUSTER_DISTRIBUTED_VIEW, "onAttachedToWindow");
        super.onAttachedToWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if(hasWindowFocus){
        }else{
            requestShutdownDrawingTask();
        }
        Log.d(LoggerTag.CLUSTER_DISTRIBUTED_VIEW, "onWindowFocusedChanged");
        super.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    protected void onDetachedFromWindow() {
        requestShutdownDrawingTask();
        Log.d(LoggerTag.CLUSTER_DISTRIBUTED_VIEW, "onDetachedFromWindow");
        super.onDetachedFromWindow();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private static class ClusterDistributedDrawTask extends Thread{
        private volatile boolean oShutdownRequested = false;
        private final SurfaceHolder oSurfaceHolder;
        private final Paint p = new Paint();
        private final FocusedDrawable oFocusedDrawable;
        private final List<ClusterDistributedDrawable> oClusterDistributedDrawableList;
        ClusterDistributedDrawTask(SurfaceHolder aSurfaceHolder, List<ClusterDistributedDrawable> aClusterDistributedDrawableList, FocusedDrawable aFocusedDrawable){
            oSurfaceHolder = aSurfaceHolder;
            oClusterDistributedDrawableList = aClusterDistributedDrawableList;
            oFocusedDrawable = aFocusedDrawable;
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

            if(oFocusedDrawable != null){
                oFocusedDrawable.draw(canvas, p);
            }

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

        public void requestShutDown(){
            oShutdownRequested = true;
            interrupt();
        }
    }
}
