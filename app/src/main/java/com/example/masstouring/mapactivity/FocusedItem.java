package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

public class FocusedItem {
    private DistributedItem oOriginalItem = null;
    private Rect oFocusedWindowRect;
    private Bitmap oBitmap;
    private int oMaxWidth;
    private int oMaxHeight;
    private boolean oEnabled = false;

    public DistributedItem getDistributedItem() {
        return oOriginalItem;
    }

    public void update(DistributedItem aOriginalItem, Context aContext, View aView) {
        Point point = new Point();
        aView.getDisplay().getRealSize(point);
        oMaxWidth = point.x;
        oMaxHeight = point.y;

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

    public boolean isEnabled(){
        return oEnabled;
    }

    public void setEnable(boolean aEnable){
        oEnabled = aEnable;
    }
}
