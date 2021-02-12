package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.masstouring.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

public class PictureClusterRenderer extends DefaultClusterRenderer<Picture> {
    private final IconGenerator oClusterIconGenerator;
    private final ImageView oClusterImageView;
    private Context oContext;
    private ClusterManager<Picture> oClusterManager;
    public PictureClusterRenderer(Context context, GoogleMap map, ClusterManager<Picture> clusterManager) {
        super(context, map, clusterManager);
        oContext = context;
        oClusterManager = clusterManager;
        oClusterIconGenerator = new IconGenerator(context);

        View multiProfile = LayoutInflater.from(context).inflate(R.layout.cluster_layout, null);
        oClusterIconGenerator.setContentView(multiProfile);
        oClusterIconGenerator.setTextAppearance(R.id.amu_text);
        oClusterImageView = multiProfile.findViewById(R.id.cluster_image);
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull Picture item, @NonNull MarkerOptions markerOptions) {
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(item.getBitmap(oContext)));
    }

    @Override
    protected void onBeforeClusterRendered(@NonNull Cluster<Picture> cluster, @NonNull MarkerOptions markerOptions) {
        oClusterImageView.setImageBitmap(cluster.getItems().stream().findFirst().get().getBitmap(oContext));
        Bitmap icon = oClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    @Override
    protected void onClusterRendered(@NonNull Cluster<Picture> cluster, @NonNull Marker marker) {
        oClusterImageView.setImageBitmap(cluster.getItems().stream().findFirst().get().getBitmap(oContext));
        Bitmap icon = oClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    @Override
    protected void onClusterItemRendered(@NonNull Picture clusterItem, @NonNull Marker marker) {
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(clusterItem.getBitmap(oContext)));
    }
}
