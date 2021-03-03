package com.example.masstouring.mapactivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

public class ClusterDistributer{
    private final ClusterManager<Picture> oClusterManager;
    private final ClusterDistributedView oClusterDistributedView;

    ClusterDistributer(Context aContext, ClusterManager<Picture> aClusterManager){
        oClusterDistributedView = new ClusterDistributedView(aContext);
        oClusterManager = aClusterManager;
    }

    public void distribute(Cluster<Picture> aCluster){

    }

    public void cleanUp(){
        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();
        if(parent != null){
            parent.removeView(oClusterDistributedView);
        }
    }

    View getClusterDistributedView(){
        return oClusterDistributedView;
    }
}
