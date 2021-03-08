package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;

import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

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

        List<Bitmap> aBitmap = aCluster.getItems().stream()
                .map(picture -> picture.getBitmapSyncly(oContext, 200,200))
                .collect(Collectors.toList());

        //calculate the layout of each item for distribution

        //draw each items on the calculated position
        oClusterDistributedView.drawItems(aBitmap);

        oMapActivitySharedViewModel.getIsClusterDistributed().setValue(true);
    }

    public void cleanUp(){
        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();
        if(parent != null){
            parent.removeView(oClusterDistributedView);
        }
        oMapActivitySharedViewModel.getIsClusterDistributed().setValue(false);
    }

    View getClusterDistributedView(){
        return oClusterDistributedView;
    }
}
