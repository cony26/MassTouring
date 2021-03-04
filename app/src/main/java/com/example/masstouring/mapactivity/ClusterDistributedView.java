package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.example.masstouring.common.LoggerTag;

import java.util.List;

public class ClusterDistributedView extends SurfaceView {
    Paint p;

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
    }

    public void drawItems(List<Bitmap> aBitmapList){
        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                SurfaceHolder holder = getHolder();
                Canvas canvas = holder.lockCanvas();
                while(canvas == null){
                    try{
                        Thread.sleep(500);
                    }catch(InterruptedException e){

                    }
                    Log.e(LoggerTag.CLUSTER, "can't lock canvas");
                    canvas = holder.lockCanvas();
                }
                Matrix matrix = new Matrix();
                for(Bitmap bitmap : aBitmapList){
                    canvas.drawBitmap(bitmap, matrix, p);
                    canvas.translate(100,100);
                }
                holder.unlockCanvasAndPost(canvas);
            }
        });

    }
}
