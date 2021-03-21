package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

import java.util.List;

public class FocusedDrawable {
    private List<DistributedItem> oDistributedItems;
    private int oIndex;
    private DistributedItem oFocusedItem = null;
    private Rect oFocusedWindowRect;
    private Bitmap oBitmap;
    private int oMaxWidth;
    private int oMaxHeight;
    private boolean oEnabled = false;

    public DistributedItem getDistributedItem() {
        return oFocusedItem;
    }

    public void update(DistributedItem aOriginalItem, List<DistributedItem> aDistributedItems, Context aContext, View aView) {
        oFocusedItem = aOriginalItem;
        oDistributedItems = aDistributedItems;
        oIndex = oDistributedItems.indexOf(oFocusedItem);

        Point point = new Point();
        aView.getDisplay().getRealSize(point);
        oMaxWidth = point.x;
        oMaxHeight = point.y;

        oBitmap = oFocusedItem.getPicture().getBitmapSynclyScaledWithin(aContext, oMaxWidth, oMaxHeight);
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

    public void draw(Canvas aCanvas, Paint p){
        if(oEnabled){
            aCanvas.drawColor(Color.BLACK);
            aCanvas.drawBitmap(oBitmap, null, oFocusedWindowRect, p);
        }
    }
}
