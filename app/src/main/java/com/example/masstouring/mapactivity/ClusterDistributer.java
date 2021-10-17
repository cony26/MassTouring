package com.example.masstouring.mapactivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;

import androidx.core.view.GestureDetectorCompat;

import com.example.masstouring.R;
import com.example.masstouring.common.LoggerTag;
import com.google.android.gms.maps.GoogleMap;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ClusterDistributer implements ClusterManager.OnClusterClickListener<Picture>, PictureClusterRenderer.IClusterUpdatedListener, IClusterDistributer {
    private final List<ClusterDistributedDrawable> oClusterDistributedDrawableList = new CopyOnWriteArrayList<>();
    private DistributedItem oTouchedItem = null;
    private final FocusedDrawable oFocusedDrawable;
    private final Context oContext;
    private final ClusterDistributedView oClusterDistributedView;
    private final MapActivtySharedViewModel oMapActivitySharedViewModel;
    private final int oClusterSquarePx;
    private final GestureDetectorCompat oDetector;
    private final PrioritizedOnBackPressedCallback oOnBackPressedWhenFocused = new PrioritizedOnBackPressedCallback(false, PrioritizedOnBackPressedCallback.CLUSTER_DISTRIBUTED_ITEM_FOCUSED) {
        @Override
        public void handleOnBackPressed() {
            oFocusedDrawable.setEnable(false);
            oOnBackPressedWhenFocused.setEnabled(false);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                oClusterDistributedView.getWindowInsetsController().show(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                oClusterDistributedView.setSystemUiVisibility(View.VISIBLE);
            }
            Log.d(LoggerTag.SYSTEM_PROCESS,"back pressed when distributed item is focused");
        }
    };

    private final View.OnTouchListener oOnTouchListener = new View.OnTouchListener() {
        private float initialX;
        private float prevX;
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if(oFocusedDrawable.isEnabled()){
                oDetector.onTouchEvent(motionEvent);

                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        initialX = motionEvent.getX();
                        prevX = initialX;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        oFocusedDrawable.updateByDistance(motionEvent.getX() - prevX);
                        prevX = motionEvent.getX();
                        return true;
                    case MotionEvent.ACTION_UP:
                        //finger move left
                        if(initialX - motionEvent.getX() > 300){
                            oFocusedDrawable.moveTo(true);
                            Log.i(LoggerTag.CLUSTER, "next FocusedDrawable");
                        //finger move right
                        }else if(initialX - motionEvent.getX() < -300){
                            oFocusedDrawable.moveTo(false);
                            Log.i(LoggerTag.CLUSTER, "previous FocusedDrawable");
                        }else{
                            //reset
                            oFocusedDrawable.updateByDistance(initialX - motionEvent.getX());
                        }
                        return true;
                    default:
                        return true;
                }
            }else{
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        return distributedItemIsTouched((int)motionEvent.getX(), (int)motionEvent.getY());
                    case MotionEvent.ACTION_UP:
                        view.performClick();
                        if(clickedItemIsSameWithTouchedItem((int)motionEvent.getX(), (int)motionEvent.getY())){
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                                oClusterDistributedView.getWindowInsetsController().hide(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                                oClusterDistributedView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                            }
                            oFocusedDrawable.prepareToShow(oTouchedItem, findGroupList(oTouchedItem), oClusterDistributedView);
                            oOnBackPressedWhenFocused.setEnabled(true);
                        }
                        oTouchedItem = null;
                        return true;
                    default:
                        return view.onTouchEvent(motionEvent);
                }
            }
        }
    };

    private final GestureDetector.SimpleOnGestureListener oSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener(){
        private boolean oTapped = false;
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if(oTapped){
                oClusterDistributedView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                oTapped = false;
            }else{
                oClusterDistributedView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                oTapped = true;
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    ClusterDistributer(Context aContext, MapActivtySharedViewModel aViewModel){
        oFocusedDrawable = new FocusedDrawable(aContext);
        oClusterDistributedView = new ClusterDistributedView(aContext, this);
        oClusterDistributedView.setOnTouchListener(oOnTouchListener);
        oDetector = new GestureDetectorCompat(aContext, oSimpleOnGestureListener);
        oContext = aContext;
        oMapActivitySharedViewModel = aViewModel;
        oClusterSquarePx = (int)aContext.getResources().getDimension(R.dimen.cluster_item_image);
        BackPressedCallbackRegisterer.getInstance().register(oOnBackPressedWhenFocused);
    }

    /**
     * attach DistributedView to {@code aActivity} if DistributedView is not yet attached.
     * @param aActivity to attach DistributedView to
     */
    public void attachDistributedView(Activity aActivity){
        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();
        if(parent == null){
            aActivity.addContentView(oClusterDistributedView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            Log.d(LoggerTag.CLUSTER_DISTRIBUTED_VIEW, "attached ClusterDistributedView");
        }else{
            Log.e(LoggerTag.CLUSTER_DISTRIBUTED_VIEW, "can't attach ClusterDistributedView unexpectedly");
        }

    }

    /**
     * detach DistributedView to {@code aActivity} if attached.
     */
    public void detachDistributedView(){
        ViewGroup parent = (ViewGroup)oClusterDistributedView.getParent();
        if(parent != null){
            parent.removeView(oClusterDistributedView);
            Log.d(LoggerTag.CLUSTER_DISTRIBUTED_VIEW, "detached ClusterDistributedView");
        }else{
            Log.e(LoggerTag.CLUSTER_DISTRIBUTED_VIEW, "can't detach ClusterDistributedView unexpectedly");
        }
        clearClusterDistributedDrawable();
        oClusterDistributedView.requestShutdownDrawingTask();
        oMapActivitySharedViewModel.isClusterDistributed().setValue(false);
    }

    @Override
    public boolean onClusterClick(Cluster<Picture> cluster) {
        if(hasClusterDistributedDrawable(cluster)){
            removeClusterDistributedDrawable(cluster);
        }else{
            distribute(cluster);
        }

        return false;
    }

    @Override
    public void onClusterCreated(Cluster<Picture> aCluster, GoogleMap aGoogleMap) {
        List<DistributedItem> renderedDistributedItems = oClusterDistributedDrawableList.stream()
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

        removeDistributedItems(distributedItems);

        Point point = aGoogleMap.getProjection().toScreenLocation(aCluster.getPosition());
        calculateDistribution(distributedItems, point.x, point.y);

        addClusterDistributedDrawable(new ClusterDistributedDrawable(new CopyOnWriteArrayList<>(distributedItems), aCluster));
        removeUnnecessaryClusterDistributedDrawable();
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
        addClusterDistributedDrawable(new ClusterDistributedDrawable(new CopyOnWriteArrayList<>(distributedItems), aCluster));

        oMapActivitySharedViewModel.isClusterDistributed().setValue(true);
    }

    public void updateClusterScreenPosition(GoogleMap aMap){
        for(ClusterDistributedDrawable drawable : oClusterDistributedDrawableList){
            Point point = aMap.getProjection().toScreenLocation(drawable.getCluster().getPosition());
            drawable.getDistributedItems().stream()
                    .forEach(item -> item.updateCenterPoint(point));
        }
    }

    public void addClusterDistributedDrawable(ClusterDistributedDrawable aClusterDistributedDrawable){
        oClusterDistributedDrawableList.add(aClusterDistributedDrawable);
        Log.i(LoggerTag.CLUSTER, "added aClusterDistributedDrawable:" + aClusterDistributedDrawable.toString() + ", clusterDistributedDrawableList:" + oClusterDistributedDrawableList);
    }

    private boolean removeUnnecessaryClusterDistributedDrawable(){
        List<ClusterDistributedDrawable> deleteList = new ArrayList<>();

        for(ClusterDistributedDrawable drawable: oClusterDistributedDrawableList){
            if(drawable.getDistributedItems().size() == 0){
                deleteList.add(drawable);
            }
        }

        return removeClusterDistributedDrawable(deleteList);
    }

    private boolean removeClusterDistributedDrawable(Cluster aCluster){
        List<ClusterDistributedDrawable> deleteList = new ArrayList<>();

        for(ClusterDistributedDrawable drawable: oClusterDistributedDrawableList){
            if(drawable.getCluster().equals(aCluster)){
                deleteList.add(drawable);
            }
        }

        return removeClusterDistributedDrawable(deleteList);
    }

    private boolean removeClusterDistributedDrawable(List<ClusterDistributedDrawable> aDeletedList){
        if(aDeletedList.isEmpty()){
            return false;
        }

        for(ClusterDistributedDrawable drawable : aDeletedList){
            oClusterDistributedDrawableList.remove(drawable);
            Log.i(LoggerTag.CLUSTER, "removed ClusterDistributedDrawable:" + drawable.toString());
        }
        return true;
    }

    private boolean removeDistributedItems(List<DistributedItem> aDistributedItems){
        boolean result = false;

        for(ClusterDistributedDrawable drawable : oClusterDistributedDrawableList){
            if(drawable.getDistributedItems().removeAll(aDistributedItems)){
                result = true;
            }
        }

        return result;
    }

    private void clearClusterDistributedDrawable(){
        oClusterDistributedDrawableList.clear();
    }

    private boolean hasClusterDistributedDrawable(Cluster<Picture> aCluster){
        return oClusterDistributedDrawableList.stream()
                .anyMatch(drawable -> drawable.getCluster().equals(aCluster));
    }

    private List<DistributedItem> findGroupList(DistributedItem aDistributedItem){
        for(ClusterDistributedDrawable drawable : oClusterDistributedDrawableList){
            List<DistributedItem> list = drawable.getDistributedItems();
            if(list.contains(aDistributedItem)){
                return list;
            }
        }

        Log.e(LoggerTag.CLUSTER, "can't findGroup unexpectedly");
        return null;
    }

    @Override
    public List<ClusterDistributedDrawable> getClusterDistributedDrawableList() {
        return oClusterDistributedDrawableList;
    }

    @Override
    public FocusedDrawable getFocusedDrawable() {
        return oFocusedDrawable;
    }

    private boolean distributedItemIsTouched(int aX, int aY){
        oTouchedItem = null;

        for(ClusterDistributedDrawable drawable : oClusterDistributedDrawableList){
            DistributedItem item = drawable.findDistributedItem(aX, aY);
            if(item != null){
                oTouchedItem = item;
                break;
            }
        }

        return oTouchedItem != null;
    }

    private boolean clickedItemIsSameWithTouchedItem(int aX, int aY) {
        if (oTouchedItem.getRect().contains(aX, aY)) {
            Log.i(LoggerTag.CLUSTER, "Distributed View is Touched:" + oTouchedItem.toString());
            return true;
        }

        return false;
    }
}
