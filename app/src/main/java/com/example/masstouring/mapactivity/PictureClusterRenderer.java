package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private final IconGenerator oClusterItemIconGenerator;
    private final ImageView oClusterImageView;
    private final ImageView oClusterItemImageView;
    private Context oContext;
    private ClusterManager<Picture> oClusterManager;
    private final int oDimension;
    public PictureClusterRenderer(Context context, GoogleMap map, ClusterManager<Picture> clusterManager) {
        super(context, map, clusterManager);
        oContext = context;
        oClusterManager = clusterManager;
        oClusterIconGenerator = new IconGenerator(context);
        oClusterItemIconGenerator = new IconGenerator(context);

        View clusterLayout = LayoutInflater.from(context).inflate(R.layout.cluster_layout, null);
        oClusterIconGenerator.setContentView(clusterLayout);
        oClusterIconGenerator.setTextAppearance(R.id.amu_text);
        oClusterImageView = clusterLayout.findViewById(R.id.cluster_image);

        oClusterItemImageView = new ImageView(context);
        oDimension = (int)context.getResources().getDimension(R.dimen.cluster_item_image);
        oClusterItemImageView.setLayoutParams(new ViewGroup.LayoutParams(oDimension,oDimension));
        int padding = (int)context.getResources().getDimension(R.dimen.cluster_item_padding);
        oClusterItemImageView.setPadding(padding, padding, padding, padding);
        oClusterItemIconGenerator.setContentView(oClusterItemImageView);
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
