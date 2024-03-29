package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.masstouring.R;
import com.example.masstouring.common.LoggerTag;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PictureClusterRenderer extends DefaultClusterRenderer<Picture> {
    private IconGenerator oClusterIconGenerator;
    private IconGenerator oItemIconGenerator;
    private ImageView oClusterImageView;
    private ImageView oItemImageView;
    private Context oContext;
    private final int oSquarePx;
    private final List<IClusterUpdatedListener> oClusterUpdatedListener = new ArrayList<>();
    private final GoogleMap oGoogleMap;

    public PictureClusterRenderer(Context context, GoogleMap map, ClusterManager<Picture> clusterManager) {
        super(context, map, clusterManager);
        oContext = context;
        oGoogleMap = map;
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
        Bitmap tmpBitmap = item.getItemBitmapAsynclyScaledOver(oContext, oSquarePx, oSquarePx, oOnCompletedLoadItemBitmapCallback);
        oItemImageView.setImageBitmap(centerRectClip(tmpBitmap, oSquarePx, oSquarePx));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(oItemIconGenerator.makeIcon()));
    }

    @Override
    protected void onClusterItemUpdated(@NonNull Picture clusterItem, @NonNull Marker marker) {
        clusterItem.getItemBitmapAsynclyScaledOver(oContext, oSquarePx, oSquarePx, oOnCompletedLoadItemBitmapCallback);
    }

    private void setItemBitmap(Bitmap aBitmap, Marker aMarker){
        oItemImageView.setImageBitmap(centerRectClip(aBitmap, oSquarePx, oSquarePx));
        if(aMarker != null){
            aMarker.setIcon(BitmapDescriptorFactory.fromBitmap(oItemIconGenerator.makeIcon()));
        }
    }

    @Override
    protected void onBeforeClusterRendered(@NonNull Cluster<Picture> cluster, @NonNull MarkerOptions markerOptions) {
        Bitmap tmpBitmap = layoutBitmapsAsyncly(cluster);
        oClusterImageView.setImageBitmap(tmpBitmap);
        Bitmap icon = oClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));

        for(IClusterUpdatedListener listener : oClusterUpdatedListener){
            listener.onClusterCreated(cluster, oGoogleMap);
        }
    }

    @Override
    protected void onClusterUpdated(@NonNull Cluster<Picture> cluster, @NonNull Marker marker) {
        layoutBitmapsAsyncly(cluster);
    }

    private void setClusterBitmap(Bitmap aBitmap, Marker aMarker, int aClusterSize){
        oClusterImageView.setImageBitmap(aBitmap);
        Bitmap icon = oClusterIconGenerator.makeIcon(String.valueOf(aClusterSize));
        if(aMarker != null){
            aMarker.setIcon(BitmapDescriptorFactory.fromBitmap(icon));
        }
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

    private Bitmap layoutBitmapsSyncly(Cluster<Picture> cluster){
        Bitmap mixBitmap = Bitmap.createBitmap(oSquarePx, oSquarePx, Bitmap.Config.ARGB_8888);
        Canvas screen = new Canvas(mixBitmap);
        List<Bitmap> bitmapList;

        switch (cluster.getSize()){
            case 2:
                // a : b
                bitmapList = cluster.getItems().stream()
                        .map(picture -> picture.getBitmapSynclyScaledOver(oContext, oSquarePx / 2, oSquarePx))
                        .map(bitmap -> centerRectClip(bitmap, oSquarePx / 2, oSquarePx))
                        .collect(Collectors.toList());

                screen.drawBitmap(bitmapList.get(0), 0, 0, null);
                screen.drawBitmap(bitmapList.get(1), oSquarePx / 2, 0, null);
                break;
            case 3:
                // a : b
                //   c
                bitmapList = cluster.getItems().stream()
                        .limit(2)
                        .map(picture -> picture.getBitmapSynclyScaledOver(oContext, oSquarePx / 2, oSquarePx / 2))
                        .map(bitmap -> centerRectClip(bitmap, oSquarePx / 2, oSquarePx / 2))
                        .collect(Collectors.toList());

                Bitmap c = cluster.getItems().stream()
                        .skip(2)
                        .limit(1)
                        .map(picture -> picture.getBitmapSynclyScaledOver(oContext, oSquarePx, oSquarePx /2))
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
                        .map(picture -> picture.getBitmapSynclyScaledOver(oContext, oSquarePx / 2, oSquarePx / 2))
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
                        .map(picture -> picture.getBitmapSynclyScaledOver(oContext, oSquarePx / 2, oSquarePx / 2))
                        .map(bitmap -> centerRectClip(bitmap, oSquarePx / 2, oSquarePx / 2))
                        .collect(Collectors.toList());

                Bitmap e = cluster.getItems().stream()
                        .skip(4)
                        .limit(1)
                        .map(picture -> picture.getBitmapSynclyScaledOver(oContext, oSquarePx / 2, oSquarePx / 2))
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

    private Bitmap layoutBitmapsAsyncly(Cluster<Picture> cluster){
        TouringMapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = layoutBitmapsSyncly(cluster);

                new Handler(Looper.getMainLooper()).post(new Runnable(){
                    @Override
                    public void run() {
                        setClusterBitmap(bitmap, getMarker(cluster), cluster.getSize());
                        Log.i(LoggerTag.CLUSTER, "set Future Cluster Bitmap");
                    }
                });

            }
        });

        Bitmap mixBitmap = Bitmap.createBitmap(oSquarePx, oSquarePx, Bitmap.Config.ARGB_8888);

        return mixBitmap;
    }

    private Picture.OnCompletedLoadBitmapCallback oOnCompletedLoadItemBitmapCallback = new Picture.OnCompletedLoadBitmapCallback() {
        @Override
        public void execute(Bitmap aBitmap, Picture aPicture) {
            setItemBitmap(aBitmap, getMarker(aPicture));
//                            aClusterManager.removeItem(Picture.this);
//                            aClusterManager.addItem(Picture.this);
//                            aClusterManager.cluster();
            Log.i(LoggerTag.CLUSTER, "set Future Item Bitmap");
        }
    };

    public void setClusterUpdatedListener(IClusterUpdatedListener aListener){
        oClusterUpdatedListener.add(aListener);
    }

    public interface IClusterUpdatedListener {
        default void onClusterUpdated(Cluster<Picture> aCluster, Marker aMarker){};
        default void onClusterCreated(Cluster<Picture> aCluster, GoogleMap aGoogleMap){};
    }
}
