package com.example.masstouring.mapactivity;

import android.content.Context;

import com.google.maps.android.clustering.ClusterManager;

public class ClusterDistributer {
    private final ClusterManager<Picture> oClusterManager;

    ClusterDistributer(Context aContext, ClusterManager<Picture> aClusterManager){
        oClusterManager = aClusterManager;
    }

    public void distribute(){

    }

    public void cleanUp(){

    }
}
