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
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;

import java.util.List;

public class ClusterDistributedView extends SurfaceView {
    private Paint p;
    private List<DistributedItem> oDistributedItems;
    private boolean oPaintable = false;
    private DistributedItem oTouchedItem = null;
    private FocusedItem oFocusedItem = null;
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
        BackPressedCallbackRegisterer.getInstance().register(oOnBackPressedWhenFocused);
    }

    private boolean isPaintable(){
        return oPaintable;
    }

    public void drawItems(List<DistributedItem> aDistributedItems){
        oPaintable = true;
        oDistributedItems = aDistributedItems;

        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                SurfaceHolder holder = getHolder();
                while(isPaintable()){
                    Canvas canvas = lockAndGetCanvas(holder);
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    for(DistributedItem item : oDistributedItems){
                        item.updateRect(Const.MOVING_RATE_PIXEL_PER_FPS);
                        Rect rect = item.getRect();
                        int padding = 10;
                        p.setColor(Color.BLUE);
                        p.setAlpha(155);
                        p.setStrokeWidth(padding);
                        canvas.drawRect(rect.left - padding, rect.top - padding, rect.right + padding, rect.bottom + padding, p);
//                        canvas.drawLine(item.getCenterX(), item.getCenterY(), rect.centerX(), rect.centerY(), p);
                        p.setAlpha(255);
                        canvas.drawBitmap(item.getBitmap(), null, item.getRect(), p);
                    }

                    paintFocusedItem(canvas);

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

        for(DistributedItem item : oDistributedItems){
            if(item.getRect().contains(aX, aY)){
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

    private void paintFocusedItem(Canvas aCanvas){
        if(oFocusedItem.isEnable()){
            aCanvas.drawBitmap(oFocusedItem.getBitmap(), null, oFocusedItem.getFocusedWindowRect(), p);
        }
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
            oPaintable = false;
        }
        super.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    protected void onDetachedFromWindow() {
        oPaintable = false;
        super.onDetachedFromWindow();
    }
}
