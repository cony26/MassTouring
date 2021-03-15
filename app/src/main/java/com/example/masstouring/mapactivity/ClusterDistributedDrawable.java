package com.example.masstouring.mapactivity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.masstouring.common.Const;
import com.google.maps.android.clustering.Cluster;

import java.util.List;

public class ClusterDistributedDrawable{
    private final List<DistributedItem> oDistributedItems;
    private final Cluster<Picture> oCluster;
    private final Paint oPaint;
    private boolean oPaintable = true;

    ClusterDistributedDrawable(List<DistributedItem> aDistributedItems, Cluster<Picture> aCluster){
        oDistributedItems = aDistributedItems;
        oCluster = aCluster;
        oPaint = new Paint();
    }

    public void draw(Canvas aCanvas){
        if(oPaintable){
            for(DistributedItem item : oDistributedItems){
                item.updateRect(Const.MOVING_RATE_PIXEL_PER_FPS);
                Rect rect = item.getRect();
                int padding = 10;
                oPaint.setColor(Color.BLUE);
                oPaint.setAlpha(155);
                oPaint.setStrokeWidth(padding);
                aCanvas.drawRect(rect.left - padding, rect.top - padding, rect.right + padding, rect.bottom + padding, oPaint);
//                        canvas.drawLine(item.getCenterX(), item.getCenterY(), rect.centerX(), rect.centerY(), p);
                oPaint.setAlpha(255);
                aCanvas.drawBitmap(item.getBitmap(), null, item.getRect(), oPaint);
            }
        }
    }

    public void setPaintable(boolean aPaintable){
        oPaintable = aPaintable;
    }

    public List<DistributedItem> getDistributedItems(){
        return oDistributedItems;
    }

    public Cluster getCluster(){
        return oCluster;
    }

    /**
     *
     * @param aX
     * @param aY
     * @return {@link DistributedItem} if (aX, aY) is included. If no item includes it, return null.
     */
    public DistributedItem findDistributedItem(int aX, int aY){
        for(DistributedItem item : oDistributedItems){
            if(item.getRect().contains(aX, aY)){
                return item;
            }
        }

        return null;
    }
}
