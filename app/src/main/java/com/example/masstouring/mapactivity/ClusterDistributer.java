package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.example.masstouring.R;
import com.example.masstouring.common.LoggerTag;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClusterDistributer{
    private final Context oContext;
    private final ClusterManager<Picture> oClusterManager;
    private final ClusterDistributedView oClusterDistributedView;
    private final MapActivtySharedViewModel oMapActivitySharedViewModel;
    private final int oClusterSquarePx;

    ClusterDistributer(Context aContext, ClusterManager<Picture> aClusterManager, MapActivtySharedViewModel aViewModel){
        oClusterDistributedView = new ClusterDistributedView(aContext);
        oContext = aContext;
        oClusterManager = aClusterManager;
        oMapActivitySharedViewModel = aViewModel;
        oClusterSquarePx = (int)aContext.getResources().getDimension(R.dimen.cluster_item_image);
    }

    public void distribute(Cluster<Picture> aCluster){
        //get cluster size

        //get item bitmap
        List<Bitmap> bitmapList = aCluster.getItems().stream()
                .map(picture -> picture.getBitmapSyncly(oContext, oClusterSquarePx / 2,oClusterSquarePx / 2))
                .collect(Collectors.toList());

        //calculate the layout of each item for distribution
        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();
        int viewCenterX = parent.getMeasuredWidth() / 2;
        int viewCenterY = parent.getMeasuredHeight() / 2;

        List<DistributedItem> distributedItems = new ArrayList<>();
        int size = bitmapList.size();
        int halfSize = size / 2;
        for(int i = 0; i < halfSize; i++){
           int r = 200;
           double theta = Math.PI / 6 * i;
           Rect rect = createRectOnCircle(bitmapList.get(i), viewCenterX, viewCenterY, r, theta);
           distributedItems.add(new DistributedItem(viewCenterX, viewCenterY, r, theta, true, bitmapList.get(i), rect));
        }

        for(int i = halfSize; i < size; i++){
            int r = 400;
            double theta = Math.PI / 6 * (i - halfSize);
            Rect rect = createRectOnCircle(bitmapList.get(i), viewCenterX, viewCenterY, r, theta);
            distributedItems.add(new DistributedItem(viewCenterX, viewCenterY, r, theta, false, bitmapList.get(i), rect));
        }

        //draw each items on the calculated position
        oClusterDistributedView.drawItems(distributedItems);

        oMapActivitySharedViewModel.getIsClusterDistributed().setValue(true);
    }

    private Rect createRectOnCircle(Bitmap aBitmap, int aCircleCenterX, int aCircleCenterY, int aRadius, double aTheta){
        int centerX = (int) (aRadius * Math.cos(aTheta)) + aCircleCenterX;
        int centerY = (int) (aRadius * Math.sin(aTheta)) + aCircleCenterY;
        int w = aBitmap.getWidth();
        int h = aBitmap.getHeight();
        return new Rect(centerX - w / 2, centerY - h / 2, centerX + w / 2, centerY + h /2);
    }

    public void cleanUp(){
        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();
        if(parent != null){
            parent.removeView(oClusterDistributedView);
        }
        oClusterDistributedView.setPaintable(false);
        oMapActivitySharedViewModel.getIsClusterDistributed().setValue(false);
    }

    View getClusterDistributedView(){
        return oClusterDistributedView;
    }
}
