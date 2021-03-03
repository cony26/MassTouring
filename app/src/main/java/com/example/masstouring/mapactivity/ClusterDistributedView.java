package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

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
                draw(surfaceHolder);
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });
    }

    private void draw(SurfaceHolder aSurfaceHolder){
        Canvas canvas = aSurfaceHolder.lockCanvas();
        canvas.drawColor(Color.BLACK);
        canvas.drawRect(100,100,400,400,p);
        aSurfaceHolder.unlockCanvasAndPost(canvas);
    }
}
