package com.example.masstouring.mapactivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.ViewGroup;

import com.example.masstouring.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ClusterDistributer implements ClusterManager.OnClusterClickListener<Picture>, PictureClusterRenderer.IClusterUpdatedListener {
    private final Context oContext;
    private final ClusterManager<Picture> oClusterManager;
    private final ClusterDistributedView oClusterDistributedView;
    private final MapActivtySharedViewModel oMapActivitySharedViewModel;
    private final int oClusterSquarePx;

    ClusterDistributer(Context aContext, ClusterManager<Picture> aClusterManager, MapActivtySharedViewModel aViewModel){
        oClusterDistributedView = new ClusterDistributedView(aContext);
        oContext = aContext;
        oClusterManager = aClusterManager;
        oMapActivitySharedViewModel = aViewModel;
        oClusterSquarePx = (int)aContext.getResources().getDimension(R.dimen.cluster_item_image);
    }

    /**
     * attach DistributedView to {@code aActivity} if DistributedView is not yet attached.
     * @param aActivity to attach DistributedView to
     */
    public void attachDistributedView(Activity aActivity){
        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();
        if(parent == null){
            aActivity.addContentView(oClusterDistributedView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    /**
     * detach DistributedView to {@code aActivity} if attached.
     */
    public void detachDistributedView(){
        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();
        if(parent != null){
            parent.removeView(oClusterDistributedView);
        }
        oClusterDistributedView.clearClusterDistributedDrawable();
        oClusterDistributedView.requestShutdownDrawingTask();
        oMapActivitySharedViewModel.getIsClusterDistributed().setValue(false);
    }

    @Override
    public boolean onClusterClick(Cluster<Picture> cluster) {
        if(oClusterDistributedView.hasClusterDistributedDrawable(cluster)){
            oClusterDistributedView.removeClusterDistributedDrawable(cluster);
        }else{
            distribute(cluster);
        }

        return false;
    }

    @Override
    public void onClusterCreated(Cluster<Picture> aCluster, GoogleMap aGoogleMap) {
        List<DistributedItem> renderedDistributedItems = oClusterDistributedView.getClusterDistributedDrawableList().stream()
                .flatMap(drawable -> drawable.getDistributedItems().stream())
                .collect(Collectors.toList());

        Collection<Picture> clusterPictureList = aCluster.getItems();

        //if new Cluster includes distributedItem, the distributed items will be included in new Cluster and the fields be updated.
        List<DistributedItem> distributedItems = new ArrayList<>();

        for(DistributedItem distributedItem : renderedDistributedItems){
            if(clusterPictureList.contains(distributedItem.getPicture())){
                distributedItems.add(distributedItem);
            }
        }

        oClusterDistributedView.removeDistributedItems(renderedDistributedItems);

        Point point = aGoogleMap.getProjection().toScreenLocation(aCluster.getPosition());
        calculateDistribution(distributedItems, point.x, point.y);

        oClusterDistributedView.addClusterDistributedDrawable(new ClusterDistributedDrawable(distributedItems, aCluster));
        oClusterDistributedView.removeUnnecessaryClusterDistributedDrawable();
    }

    private void calculateDistribution(List<DistributedItem> aDistributedItems, int aCenterX, int aCenterY){
        //calculate the layout of each item for distribution
        int size = aDistributedItems.size();
        int halfSize = size / 2;
        for(int i = 0; i < halfSize; i++){
            int r = 300;
            double theta = Math.PI / 3 * i;
            aDistributedItems.get(i).update(aCenterX, aCenterY, r, theta, true);
        }

        for(int i = halfSize; i < size; i++){
            int r = 500;
            double theta = Math.PI / 6 * (i - halfSize);
            aDistributedItems.get(i).update(aCenterX, aCenterY, r, theta, false);
        }
    }

    public void distribute(Cluster<Picture> aCluster){
        List<Picture> pictureList = new ArrayList<>(aCluster.getItems());

        //get item bitmap
        List<Bitmap> bitmapList = pictureList.stream()
                .map(picture -> picture.getBitmapSynclyScaledOver(oContext, oClusterSquarePx / 2,oClusterSquarePx / 2))
                .collect(Collectors.toList());

        //calculate the layout of each item for distribution
        List<DistributedItem> distributedItems = new ArrayList<>();
        int size = pictureList.size();
        for(int i = 0; i < size; i++){
            distributedItems.add(new DistributedItem(bitmapList.get(i), pictureList.get(i)));
        }

        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();
        int viewCenterX = parent.getMeasuredWidth() / 2;
        int viewCenterY = parent.getMeasuredHeight() / 2;
        calculateDistribution(distributedItems, viewCenterX, viewCenterY);

        //draw each items on the calculated position
        oClusterDistributedView.addClusterDistributedDrawable(new ClusterDistributedDrawable(distributedItems, aCluster));

        oMapActivitySharedViewModel.getIsClusterDistributed().setValue(true);
    }

    public void updateClusterScreenPosition(GoogleMap aMap){
        List<ClusterDistributedDrawable> list = oClusterDistributedView.getClusterDistributedDrawableList();
        for(ClusterDistributedDrawable drawable : list){
            Point point = aMap.getProjection().toScreenLocation(drawable.getCluster().getPosition());
            drawable.getDistributedItems().stream()
                    .forEach(item -> item.updateCenterPoint(point));
        }
    }
}
