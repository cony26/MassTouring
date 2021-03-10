package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.masstouring.common.LoggerTag;

import java.util.List;

public class ClusterDistributedView extends SurfaceView {
    Paint p;
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
        p.setColor(Color.CYAN);
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

    public void drawItems(List<Bitmap> aBitmapList){
        oPaintable = true;
        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                int x = 0;
                int y = 0;
                SurfaceHolder holder = getHolder();
                while(oPaintable){
                    Canvas canvas = holder.lockCanvas();
                    while(canvas == null){
                        Log.i(LoggerTag.CLUSTER, "can't lock canvas. wait getting lock...");
                        try{
                            Thread.sleep(10);
                        }catch(InterruptedException e){
                            Log.e(LoggerTag.CLUSTER, "InterruptedException:", e);
                        }
                        canvas = holder.lockCanvas();
                    }
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    Matrix matrix = new Matrix();
                    for(Bitmap bitmap : aBitmapList){
                        canvas.drawBitmap(bitmap, matrix, p);
                        canvas.translate(x + 100,y + 100);
                    }
                    holder.unlockCanvasAndPost(canvas);

                    try{
                        Thread.sleep(30);
                    }catch(InterruptedException e){
                        Log.e(LoggerTag.CLUSTER, "InterruptedException:", e);
                    }
                    x += 50;
                    y += 50;
                }
            }
        });

    }
}
