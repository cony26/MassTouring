package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

public class FocusedItem {
    private DistributedItem oOriginalItem = null;
    private Rect oFocusedWindowRect;
    private Bitmap oBitmap;
    private final int oMaxWidth;
    private final int oMaxHeight;
    private boolean oEnabled = false;

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
        oEnabled = true;
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

    public boolean isEnabled(){
        return oEnabled;
    }

    public void setEnable(boolean aEnable){
        oEnabled = aEnable;
    }
}
