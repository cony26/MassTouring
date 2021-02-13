package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.masstouring.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.util.List;
import java.util.stream.Collectors;

public class PictureClusterRenderer extends DefaultClusterRenderer<Picture> {
    private final IconGenerator oClusterIconGenerator;
    private final IconGenerator oItemIconGenerator;
    private final ImageView oClusterImageView;
    private final ImageView oItemImageView;
    private Context oContext;
    private final int oSquarePx;
    public PictureClusterRenderer(Context context, GoogleMap map, ClusterManager<Picture> clusterManager) {
        super(context, map, clusterManager);
        oContext = context;
        oClusterIconGenerator = new IconGenerator(context);
        oItemIconGenerator = new IconGenerator(context);
        oSquarePx = (int)context.getResources().getDimension(R.dimen.cluster_item_image);

        //cluster
        View clusterLayout = LayoutInflater.from(context).inflate(R.layout.cluster_layout, null);
        oClusterIconGenerator.setContentView(clusterLayout);
        oClusterIconGenerator.setTextAppearance(R.id.amu_text);
        int bubbleColor = ContextCompat.getColor(context, R.color.bubble_color);
        oClusterIconGenerator.setColor(bubbleColor);
        oClusterImageView = clusterLayout.findViewById(R.id.cluster_image);

        //clusterItem
        oItemImageView = new ImageView(context);
        oItemImageView.setLayoutParams(new ViewGroup.LayoutParams(oSquarePx, oSquarePx));
        int padding = (int)context.getResources().getDimension(R.dimen.cluster_item_padding);
        oItemImageView.setPadding(padding, padding, padding, padding);
        oItemIconGenerator.setContentView(oItemImageView);
        oItemIconGenerator.setColor(bubbleColor);
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull Picture item, @NonNull MarkerOptions markerOptions) {
        Bitmap bitmap = item.getBitmap(oContext, oSquarePx, oSquarePx);
        if(bitmap == null){
            return;
        }

        oItemImageView.setImageBitmap(centerClip(bitmap, oSquarePx, oSquarePx));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(oItemIconGenerator.makeIcon()));
    }

    @Override
    protected void onClusterItemUpdated(@NonNull Picture clusterItem, @NonNull Marker marker) {
        Bitmap bitmap = clusterItem.getBitmap(oContext, oSquarePx, oSquarePx);
        if(bitmap == null){
            return;
        }

        oItemImageView.setImageBitmap(centerClip(bitmap, oSquarePx, oSquarePx));
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(oItemIconGenerator.makeIcon()));
    }

    @Override
    protected void onBeforeClusterRendered(@NonNull Cluster<Picture> cluster, @NonNull MarkerOptions markerOptions) {
        Bitmap bitmap = layoutBitmaps(cluster);
        oClusterImageView.setImageBitmap(bitmap);
        Bitmap icon = oClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    @Override
    protected void onClusterUpdated(@NonNull Cluster<Picture> cluster, @NonNull Marker marker) {
        Bitmap bitmap = layoutBitmaps(cluster);
        oClusterImageView.setImageBitmap(bitmap);
        Bitmap icon = oClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    @Override
    protected boolean shouldRenderAsCluster(@NonNull Cluster<Picture> cluster) {
        return cluster.getSize() > 1;
    }

    private Bitmap centerClip(Bitmap aBitmap, int aClipWidth, int aClipHeight){
        int width = aBitmap.getWidth();
        int height = aBitmap.getHeight();
        boolean landscape = width > height;
        int x;
        int y;
        if(landscape){
            x = (width - aClipWidth) / 2;
            y = 0;
        }else{
            x = 0;
            y = (height - aClipHeight) / 2;
        }
        return Bitmap.createBitmap(aBitmap, x, y, aClipWidth, aClipHeight);
    }

    private Bitmap layoutBitmaps(Cluster<Picture> cluster){
        Bitmap mixBitmap = Bitmap.createBitmap(oSquarePx, oSquarePx, Bitmap.Config.ARGB_8888);
        Canvas screen = new Canvas(mixBitmap);
        List<Bitmap> bitmapList;

        switch (cluster.getSize()){
            case 2:
                // a : b
                bitmapList = cluster.getItems().stream()
                        .map(picture -> picture.getBitmap(oContext, oSquarePx / 2, oSquarePx))
                        .map(bitmap -> centerClip(bitmap, oSquarePx / 2, oSquarePx))
                        .collect(Collectors.toList());

                screen.drawBitmap(bitmapList.get(0), 0, 0, null);
                screen.drawBitmap(bitmapList.get(1), oSquarePx / 2, 0, null);
                break;
            case 3:
                // a : b
                //   c
                List<Picture> pictureList = cluster.getItems().stream().collect(Collectors.toList());

                Bitmap a = pictureList.get(0).getBitmap(oContext, oSquarePx / 2, oSquarePx / 2);
                a = centerClip(a, oSquarePx / 2, oSquarePx / 2);

                Bitmap b = pictureList.get(1).getBitmap(oContext, oSquarePx / 2, oSquarePx / 2);
                b = centerClip(b, oSquarePx / 2, oSquarePx / 2);

                Bitmap c = pictureList.get(2).getBitmap(oContext, oSquarePx, oSquarePx /2);
                c = centerClip(c, oSquarePx, oSquarePx / 2);

                screen.drawBitmap(a, 0, 0, null);
                screen.drawBitmap(b, oSquarePx / 2, 0, null);
                screen.drawBitmap(c, 0, oSquarePx /2, null);
                break;
            default:
                // a : b
                // c : d
                bitmapList = cluster.getItems().stream()
                        .map(picture -> picture.getBitmap(oContext, oSquarePx / 2, oSquarePx / 2))
                        .map(bitmap -> centerClip(bitmap, oSquarePx / 2, oSquarePx / 2))
                        .collect(Collectors.toList());

                screen.drawBitmap(bitmapList.get(0), 0, 0, null);
                screen.drawBitmap(bitmapList.get(1), oSquarePx / 2, 0, null);
                screen.drawBitmap(bitmapList.get(2), 0, oSquarePx / 2, null);
                screen.drawBitmap(bitmapList.get(3), oSquarePx / 2, oSquarePx / 2, null);
                break;
        }

        return mixBitmap;
    }

}
