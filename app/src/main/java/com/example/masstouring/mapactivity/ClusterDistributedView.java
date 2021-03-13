package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;

import java.util.List;

public class ClusterDistributedView extends SurfaceView {
    private Paint p;
    private List<DistributedItem> oDistributedItems;
    private boolean oPaintable = false;

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
        p = new Paint();
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
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

    public void setPaintable(boolean aPaintable){
        oPaintable = aPaintable;
    }

    public void drawItems(List<DistributedItem> aDistributedItems){
        oPaintable = true;
        oDistributedItems = aDistributedItems;

        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                SurfaceHolder holder = getHolder();
                while(oPaintable){
                    Canvas canvas = lockAndGetCanvas(holder);
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    for(DistributedItem item : oDistributedItems){
                        item.updateRect(Const.MOVING_RATE_PIXEL_PER_FPS);
                        canvas.drawBitmap(item.getBitmap(), null, item.getRect(), p);
                    }

                    holder.unlockCanvasAndPost(canvas);
                    waitForFpsTime();
                }
            }
        });

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

    private void waitForFpsTime(){
        try{
            Thread.sleep(Const.FPS_MILLIS);
        }catch(InterruptedException e){
            Log.e(LoggerTag.CLUSTER, "InterruptedException:", e);
        }
    }

    public List<DistributedItem> getDistributedItems(){
        return oDistributedItems;
    }
}
