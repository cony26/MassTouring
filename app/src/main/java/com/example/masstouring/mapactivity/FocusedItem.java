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
        oBitmap = oOriginalItem.getPicture().getBitmapSyncly(aContext, oMaxWidth, oMaxHeight);
        int centerX = oMaxWidth / 2;
        int centerY = oMaxHeight / 2;
        int halfWidth = oBitmap.getWidth();
        int halfHeight = oBitmap.getHeight();
        oFocusedWindowRect = new Rect(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
        oEnable = true;
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
