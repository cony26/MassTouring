package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.example.masstouring.common.LoggerTag;

public class FocusedItem {
    private DistributedItem oOriginalItem = null;
    private Rect oFocusedWindowRect;
    private Bitmap oBitmap;
    private final int oMaxWidth;
    private final int oMaxHeight;
    private boolean oEnable = false;

    FocusedItem(int aMaxWidth, int aMaxHeight){
        oMaxWidth = aMaxWidth;
        oMaxHeight = aMaxHeight;
    }

    public DistributedItem getDistributedItem() {
        return oOriginalItem;
    }

    public void update(DistributedItem aOriginalItem, Context aContext) {
        oOriginalItem = aOriginalItem;
        oBitmap = oOriginalItem.getPicture().getBitmapSynclyScaledWithin(aContext, oMaxWidth, oMaxHeight);
        int centerX = oMaxWidth / 2;
        int centerY = oMaxHeight / 2;
        int halfWidth = oBitmap.getWidth() / 2;
        int halfHeight = oBitmap.getHeight() / 2;
        oFocusedWindowRect = new Rect(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
        oEnable = true;
        Log.e(LoggerTag.CLUSTER, String.format("(x,y)=(%d,%d), (w,h)=(%d,%d)", centerX, centerY, halfWidth*2, halfHeight*2));
    }

    public Rect getFocusedWindowRect() {
        return oFocusedWindowRect;
    }

    public Bitmap getBitmap() {
        return oBitmap;
    }

    public void setBitmap(Bitmap aBitmap) {
        oBitmap = aBitmap;
    }

    public boolean isEnable(){
        return oEnable;
    }

    public void setEnable(boolean aEnable){
        oEnable = aEnable;
    }
}
