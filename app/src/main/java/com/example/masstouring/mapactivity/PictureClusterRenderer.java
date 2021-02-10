package com.example.masstouring.mapactivity;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class PictureClusterRenderer extends DefaultClusterRenderer<Picture> {
    private Context oContext;
    private ClusterManager<Picture> oClusterManager;
    public PictureClusterRenderer(Context context, GoogleMap map, ClusterManager<Picture> clusterManager) {
        super(context, map, clusterManager);
        oContext = context;
        oClusterManager = clusterManager;
    }

    @NonNull
    @Override
    protected BitmapDescriptor getDescriptorForCluster(@NonNull Cluster<Picture> cluster) {
        return super.getDescriptorForCluster(cluster);
//        return BitmapDescriptorFactory.fromBitmap(cluster.getItems().stream().findFirst().get().getBitmap(oContext));
    }

    @Override
    protected void onClusterRendered(@NonNull Cluster<Picture> cluster, @NonNull Marker marker) {
        oClusterManager.getMarkerCollection().addMarker(new MarkerOptions()
                .position(cluster.getPosition())
                .alpha((float)0.5)
                .icon(BitmapDescriptorFactory.fromBitmap(cluster.getItems().stream().findFirst().get().getBitmap(oContext))));
    }

    @Override
    protected void onClusterItemRendered(@NonNull Picture clusterItem, @NonNull Marker marker) {
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(clusterItem.getBitmap(oContext)));
    }
}
