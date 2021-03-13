package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

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

    ClusterDistributer(Context aContext, ClusterManager<Picture> aClusterManager, MapActivtySharedViewModel aViewModel){
        oClusterDistributedView = new ClusterDistributedView(aContext);
        oContext = aContext;
        oClusterManager = aClusterManager;
        oMapActivitySharedViewModel = aViewModel;
    }

    public void distribute(Cluster<Picture> aCluster){
        //get cluster size

        //get item bitmap
        List<Bitmap> bitmapList = aCluster.getItems().stream()
                .map(picture -> picture.getBitmapSyncly(oContext, 200,200))
                .collect(Collectors.toList());

        //calculate the layout of each item for distribution
        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();

        List<Rect> distributedRectList = new ArrayList<>();
        int viewCenterX = parent.getMeasuredWidth() / 2;
        int viewCenterY = parent.getMeasuredWidth() / 2;

        Log.e(LoggerTag.CLUSTER, String.format("viewCenter (x,y) = (%d, %d)", viewCenterX, viewCenterY));

        int size = bitmapList.size();
        int halfSize = size / 2;
        for(int i = 0; i < halfSize; i++){
           int r = 200;
           double theta = Math.PI / 3 * i;
           int centerX = (int) (r * Math.cos(theta)) + viewCenterX;
           int centerY = (int) (r * Math.sin(theta)) + viewCenterY;
           int w = bitmapList.get(i).getWidth();
           int h = bitmapList.get(i).getHeight();

            Log.e(LoggerTag.CLUSTER, String.format("(w,h,i) = (%d, %d, %d)", w, h, i));
            distributedRectList.add(new Rect(centerX - w / 2, centerY - h / 2, centerX + w / 2, centerY + h /2));
        }

        for(int i = halfSize; i < size; i++){
            int r = 400;
            double theta = Math.PI / 3 * i;
            int centerX = (int) (r * Math.cos(theta)) + viewCenterX;
            int centerY = (int) (r * Math.sin(theta)) + viewCenterY;
            int w = bitmapList.get(i).getWidth();
            int h = bitmapList.get(i).getHeight();
            Log.e(LoggerTag.CLUSTER, String.format("(w,h,i) = (%d, %d, %d)", w, h, i));
            distributedRectList.add(new Rect(centerX - w / 2, centerY - h / 2, centerX + w / 2, centerY + h /2));
        }

        for(Rect rect : distributedRectList){
            Log.e(LoggerTag.CLUSTER, String.format("(left, top, right, bottom) = (%d, %d, %d, %d)", rect.left, rect.top, rect.right, rect.bottom));
        }

        //draw each items on the calculated position
        oClusterDistributedView.drawItems(bitmapList, distributedRectList);

        oMapActivitySharedViewModel.getIsClusterDistributed().setValue(true);
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
