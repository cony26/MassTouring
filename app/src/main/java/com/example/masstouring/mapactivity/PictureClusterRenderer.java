package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.provider.DocumentsContract;
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
        oItemImageView.setImageBitmap(centerRectClip(bitmap, oSquarePx, oSquarePx));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(oItemIconGenerator.makeIcon()));
    }

    @Override
    protected void onClusterItemUpdated(@NonNull Picture clusterItem, @NonNull Marker marker) {
        Bitmap bitmap = clusterItem.getBitmap(oContext, oSquarePx, oSquarePx);
        oItemImageView.setImageBitmap(centerRectClip(bitmap, oSquarePx, oSquarePx));
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

    private Bitmap centerRectClip(Bitmap aBitmap, int aClipWidth, int aClipHeight){
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

    private Bitmap centerCircleClip(Bitmap aBitmap, int aClipRadius){
        int width = aBitmap.getWidth();
        int height = aBitmap.getHeight();
        int diameter = aClipRadius * 2;
        Bitmap newBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        Path path = new Path();
        path.addOval(new RectF(0,0, diameter,diameter), Path.Direction.CW);
        canvas.clipPath(path);
        canvas.drawBitmap(aBitmap, - (width / 2 - aClipRadius), -(height / 2 - aClipRadius), null);

        return newBitmap;
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
                        .map(bitmap -> centerRectClip(bitmap, oSquarePx / 2, oSquarePx))
                        .collect(Collectors.toList());

                screen.drawBitmap(bitmapList.get(0), 0, 0, null);
                screen.drawBitmap(bitmapList.get(1), oSquarePx / 2, 0, null);
                break;
            case 3:
                // a : b
                //   c
                List<Picture> pictureList = cluster.getItems().stream().collect(Collectors.toList());

                bitmapList = cluster.getItems().stream()
                        .limit(2)
                        .map(picture -> picture.getBitmap(oContext, oSquarePx / 2, oSquarePx / 2))
                        .map(bitmap -> centerRectClip(bitmap, oSquarePx / 2, oSquarePx / 2))
                        .collect(Collectors.toList());

                Bitmap c = cluster.getItems().stream()
                        .skip(2)
                        .limit(1)
                        .map(picture -> picture.getBitmap(oContext, oSquarePx, oSquarePx /2))
                        .map(bitmap -> centerRectClip(bitmap, oSquarePx, oSquarePx / 2))
                        .findFirst()
                        .get();

                screen.drawBitmap(bitmapList.get(0), 0, 0, null);
                screen.drawBitmap(bitmapList.get(1), oSquarePx / 2, 0, null);
                screen.drawBitmap(c, 0, oSquarePx /2, null);
                break;
            case 4:
                // a : b
                // c : d
                bitmapList = cluster.getItems().stream()
                        .limit(4)
                        .map(picture -> picture.getBitmap(oContext, oSquarePx / 2, oSquarePx / 2))
                        .map(bitmap -> centerRectClip(bitmap, oSquarePx / 2, oSquarePx / 2))
                        .collect(Collectors.toList());

                screen.drawBitmap(bitmapList.get(0), 0, 0, null);
                screen.drawBitmap(bitmapList.get(1), oSquarePx / 2, 0, null);
                screen.drawBitmap(bitmapList.get(2), 0, oSquarePx / 2, null);
                screen.drawBitmap(bitmapList.get(3), oSquarePx / 2, oSquarePx / 2, null);
                break;
            default:
                // a : b
                //  (e)
                // c : d
                bitmapList = cluster.getItems().stream()
                        .limit(4)
                        .map(picture -> picture.getBitmap(oContext, oSquarePx / 2, oSquarePx / 2))
                        .map(bitmap -> centerRectClip(bitmap, oSquarePx / 2, oSquarePx / 2))
                        .collect(Collectors.toList());

                Bitmap e = cluster.getItems().stream()
                        .skip(4)
                        .limit(1)
                        .map(picture -> picture.getBitmap(oContext, oSquarePx / 2, oSquarePx / 2))
                        .map(bitmap -> centerCircleClip(bitmap, oSquarePx / 4))
                        .findFirst()
                        .get();

                screen.drawBitmap(bitmapList.get(0), 0, 0, null);
                screen.drawBitmap(bitmapList.get(1), oSquarePx / 2, 0, null);
                screen.drawBitmap(bitmapList.get(2), 0, oSquarePx / 2, null);
                screen.drawBitmap(bitmapList.get(3), oSquarePx / 2, oSquarePx / 2, null);
                screen.drawBitmap(e, oSquarePx / 4, oSquarePx / 4, null);
        }

        return mixBitmap;
    }

}
