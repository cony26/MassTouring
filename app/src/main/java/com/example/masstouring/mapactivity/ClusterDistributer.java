package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import com.example.masstouring.R;
import com.google.android.gms.maps.GoogleMap;
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
    private Cluster<Picture> oCluster;

    ClusterDistributer(Context aContext, ClusterManager<Picture> aClusterManager, MapActivtySharedViewModel aViewModel){
        oClusterDistributedView = new ClusterDistributedView(aContext);
        oContext = aContext;
        oClusterManager = aClusterManager;
        oMapActivitySharedViewModel = aViewModel;
        oClusterSquarePx = (int)aContext.getResources().getDimension(R.dimen.cluster_item_image);
    }

    public void distribute(Cluster<Picture> aCluster){
        oCluster = aCluster;
        List<Picture> pictureList = new ArrayList<>(oCluster.getItems());

        //get item bitmap
        List<Bitmap> bitmapList = pictureList.stream()
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
           int r = 300;
           double theta = Math.PI / 3 * i;
           Rect rect = createRectOnCircle(bitmapList.get(i), viewCenterX, viewCenterY, r, theta);
           distributedItems.add(new DistributedItem(viewCenterX, viewCenterY, r, theta, true, bitmapList.get(i), rect, pictureList.get(i)));
        }

        for(int i = halfSize; i < size; i++){
            int r = 500;
            double theta = Math.PI / 6 * (i - halfSize);
            Rect rect = createRectOnCircle(bitmapList.get(i), viewCenterX, viewCenterY, r, theta);
            distributedItems.add(new DistributedItem(viewCenterX, viewCenterY, r, theta, false, bitmapList.get(i), rect, pictureList.get(i)));
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

    public void updateClusterScreenPosition(GoogleMap aMap){
        Point point = aMap.getProjection().toScreenLocation(oCluster.getPosition());
        oClusterDistributedView.getDistributedItems().stream()
                .forEach(item -> item.updateCenterPoint(point));
    }
}
