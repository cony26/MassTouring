package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
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
    private final int oDimension;
    public PictureClusterRenderer(Context context, GoogleMap map, ClusterManager<Picture> clusterManager) {
        super(context, map, clusterManager);
        oContext = context;
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
        Bitmap bitmap = item.getBitmap(oContext, oDimension, oDimension);
        if(bitmap == null){
            return;
        }

        oClusterItemImageView.setImageBitmap(centerClipWithDimension(bitmap));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(oClusterItemIconGenerator.makeIcon()));
    }

    @Override
    protected void onClusterItemUpdated(@NonNull Picture clusterItem, @NonNull Marker marker) {
        Bitmap bitmap = clusterItem.getBitmap(oContext, oDimension, oDimension);
        if(bitmap == null){
            return;
        }

        oClusterItemImageView.setImageBitmap(centerClipWithDimension(bitmap));
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(oClusterItemIconGenerator.makeIcon()));
    }

    @Override
    protected void onBeforeClusterRendered(@NonNull Cluster<Picture> cluster, @NonNull MarkerOptions markerOptions) {
        oClusterImageView.setImageBitmap(cluster.getItems().stream().findFirst().get().getBitmap(oContext, oDimension, oDimension));
        Bitmap icon = oClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    @Override
    protected void onClusterUpdated(@NonNull Cluster<Picture> cluster, @NonNull Marker marker) {
        oClusterImageView.setImageBitmap(cluster.getItems().stream().findFirst().get().getBitmap(oContext, oDimension, oDimension));
        Bitmap icon = oClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    private Bitmap centerClipWithDimension(Bitmap aBitmap){
        int width = aBitmap.getWidth();
        int height = aBitmap.getHeight();
        boolean landscape = width > height;
        int x;
        int y;
        if(landscape){
            x = (width - oDimension) / 2;
            y = 0;
        }else{
            x = 0;
            y = (height - oDimension) / 2;
        }
        return Bitmap.createBitmap(aBitmap, x, y, oDimension, oDimension);
    }


}
