package com.example.masstouring.mapactivity;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masstouring.R;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.event.FitAreaEvent;
import com.example.masstouring.event.PolylineRenderEvent;
import com.example.masstouring.event.RemoveRecordItemEvent;
import com.example.masstouring.mapactivity.DeleteConfirmationDialog.IDeleteConfirmationDialogCallback;
import com.example.masstouring.viewmodel.MapActivtySharedViewModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BoundRecordView implements LifecycleObserver, IItemClickCallback{
    private final RecyclerView oRecordsView;
    private RecordsViewAdapter oRecordsViewAdapter;
    private final MapActivtySharedViewModel oMapActivitySharedViewModel;
    private final PrioritizedOnBackPressedCallback oOnBackPressedCallbackWhenViewVisible = new PrioritizedOnBackPressedCallback(false, PrioritizedOnBackPressedCallback.RECORD_VIEW) {
        @Override
        public void handleOnBackPressed() {
            oMapActivitySharedViewModel.isRecordsViewVisible().setValue(false);
            Log.d(LoggerTag.SYSTEM_PROCESS,"back pressed when records view visible");
        }
    };

    public final IDeleteConfirmationDialogCallback oDeleteDialogCallback = new IDeleteConfirmationDialogCallback() {
        @Override
        public void onPositiveClick() {
            oMapActivitySharedViewModel.onDeletePositiveClick(BoundRecordView.this);
        }

        @Override
        public void onNegativeClick() {
        }
    };

    public BoundRecordView(FragmentActivity aActivity){
        aActivity.getLifecycle().addObserver(this);
        oRecordsView = aActivity.findViewById(R.id.recordsView);
        LinearLayoutManager oManager = new LinearLayoutManager(oRecordsView.getContext());
        oMapActivitySharedViewModel = new ViewModelProvider(aActivity).get(MapActivtySharedViewModel.class);
        oManager.setOrientation(LinearLayoutManager.VERTICAL);
        oRecordsView.setLayoutManager(oManager);
        oRecordsView.setVisibility(View.GONE);

        subscribe(aActivity);
        BackPressedCallbackRegisterer.getInstance().register(oOnBackPressedCallbackWhenViewVisible);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    void initialize(){

    }

    private void subscribe(LifecycleOwner aLifeCycleOwner){
        oMapActivitySharedViewModel.isRecordsViewVisible().observe(aLifeCycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isVisible) {
                if(isVisible){
                    List<RecordItem> data = loadRecords();
                    oRecordsViewAdapter = new RecordsViewAdapter(data, BoundRecordView.this, oRecordsView.getContext());
                    oRecordsView.setAdapter(oRecordsViewAdapter);
                    oRecordsView.setVisibility(View.VISIBLE);
                    oOnBackPressedCallbackWhenViewVisible.setEnabled(true);
                }else{
                    oRecordsView.setVisibility(View.GONE);
                    oOnBackPressedCallbackWhenViewVisible.setEnabled(false);
                    oMapActivitySharedViewModel.getDeleteRecordsIconVisible().setValue(false);
                }
            }
        });
    }

    private List<RecordItem> loadRecords(){
        RecordItem recordItemArray[] = new RecordItem[oMapActivitySharedViewModel.getRecordSize()];
        Arrays.fill(recordItemArray, RecordItem.EMPTY_RECORD);

        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                List<RecordItem> data = oMapActivitySharedViewModel.getRecords();
                data.stream()
                        .filter(recordItem -> oMapActivitySharedViewModel.isRendered(recordItem.getId()))
                        .forEach(recordItem -> recordItem.setRendered(true));
                while(oRecordsViewAdapter == null){
                    try{
                        Thread.sleep(100);
                    }catch(InterruptedException e){
                        Log.d(LoggerTag.RECORD_RECYCLER_VIEW, "interrupted");
                    }
                }
                oRecordsViewAdapter.setData(data);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        oRecordsViewAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        return Arrays.asList(recordItemArray);
    }

    @Override
    public void onRecordItemClick(RecordItem aRecordItem) {
        if(aRecordItem == RecordItem.EMPTY_RECORD){
            return ;
        }
        Map locationMap = aRecordItem.getLocationMap();
        if(locationMap.size() <= 1)
            return;

        if(aRecordItem.isRendered()){
            oMapActivitySharedViewModel.getRemoveRecordItemEvent().setValue(new RemoveRecordItemEvent(aRecordItem));
        }else{
            oMapActivitySharedViewModel.getFitAreaEvent().setValue(new FitAreaEvent(aRecordItem));
            oMapActivitySharedViewModel.getPolylineRenderEvent().setValue(new PolylineRenderEvent(aRecordItem));
        }

        oMapActivitySharedViewModel.isTracePosition().setValue(false);
    }

    @Override
    public void onRecordItemLongClick() {
        if(oRecordsViewAdapter.getSelectedItemIdList().size() > 0){
            oMapActivitySharedViewModel.getDeleteRecordsIconVisible().setValue(true);
        }else{
            oMapActivitySharedViewModel.getDeleteRecordsIconVisible().setValue(false);
        }
    }

    public void deleteSelectedItems(){
        List<Integer> list = oRecordsViewAdapter.getSelectedItemIdList();
        oMapActivitySharedViewModel.deleteRecord(list.stream().mapToInt(id -> id).toArray());
        oRecordsViewAdapter.setData(oMapActivitySharedViewModel.getRecords());
        oRecordsViewAdapter.notifyDataSetChanged();
    }
}
