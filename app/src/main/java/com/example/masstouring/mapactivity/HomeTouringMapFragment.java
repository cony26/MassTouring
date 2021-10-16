package com.example.masstouring.mapactivity;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.masstouring.R;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LifeCycleLogger;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.common.LoggerTask;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.database.DatabaseInfoRepairer;
import com.example.masstouring.recordservice.RecordService;
import com.google.android.gms.maps.SupportMapFragment;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeTouringMapFragment extends Fragment {
    static final ExecutorService cExecutors = Executors.newFixedThreadPool(5);
    private Button oStartRecordingButton;
    private Button oMemoryButton;
    private BoundRecordView oBoundRecordsView;
    private Toolbar oToolbar;
    private BoundMapFragment oBoundMapFragment;
    private MapActivtySharedViewModel oMapActivitySharedViewModel;
    private RecordService oRecordService;
    private boolean oRecordServiceBound = false;
    private DatabaseHelper oDatabaseHelper;

    private DeleteConfirmationDialog.IDeleteConfirmationDialogCallback oDeleteDialogCallback = new DeleteConfirmationDialog.IDeleteConfirmationDialogCallback() {
        @Override
        public void onPositiveClick() {
            oBoundRecordsView.deleteSelectedItems();
            oMapActivitySharedViewModel.getToolbarVisibility().setValue(View.GONE);
        }

        @Override
        public void onNegativeClick() {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoggerTask.getInstance().setMapActivityState(true);
        new LifeCycleLogger(this, getClass().getSimpleName());
        BackPressedCallbackRegisterer.initialize((AppCompatActivity)getActivity());
        checkPermissions();
        oMapActivitySharedViewModel = new ViewModelProvider(this).get(MapActivtySharedViewModel.class);
        oDatabaseHelper = new DatabaseHelper(getContext(), Const.DB_NAME);

        cExecutors.execute(new DatabaseInfoRepairer(oDatabaseHelper));
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.home_touring_map_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        oStartRecordingButton = view.findViewById(R.id.btnStartRecording);
        oMemoryButton = view.findViewById(R.id.btnMemory);
        oToolbar = view.findViewById(R.id.toolbar);
        setButtonClickListeners();

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        oBoundMapFragment = new BoundMapFragment(activity, oMapActivitySharedViewModel, (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.map), oDatabaseHelper);
        oBoundRecordsView = new BoundRecordView(activity, view.findViewById(R.id.recordsView), oMapActivitySharedViewModel, oDatabaseHelper);
        oBoundRecordsView.setMapFragment(oBoundMapFragment);
        subscribeLiveData();
    }

    @Override
    public void onStart() {
        startRecordService();
        super.onStart();
    }

    @Override
    public void onPause(){
        super.onPause();
        stopServiceIfNotRecording();
        unbindServiceGracefully();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LoggerTask.getInstance().setMapActivityState(false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(LoggerTag.SYSTEM_PROCESS,"onSaveInstanceState MapActivity");
        super.onSaveInstanceState(outState);
    }

    private void subscribeLiveData(){
        LifecycleOwner owner = getViewLifecycleOwner();
        oMapActivitySharedViewModel.getRecordState().observe(owner, new Observer<RecordState>() {
            @Override
            public void onChanged(RecordState recordState) {
                oStartRecordingButton.setText(recordState.getButtonStringId());
            }
        });

        oMapActivitySharedViewModel.getRecordStartEvent().observe(owner, new Observer<RecordStartEvent>() {
            @Override
            public void onChanged(RecordStartEvent recordStartEvent) {
                Optional.ofNullable(recordStartEvent.getContentIfNotHandled()).ifPresent(content -> {
                    Toast.makeText(getContext(), content, Toast.LENGTH_SHORT).show();
                    oBoundMapFragment.initialize();
                    oMapActivitySharedViewModel.getIsRecordsViewVisible().setValue(false);
                });
            }
        });

        oMapActivitySharedViewModel.getRecordEndEvent().observe(owner, new Observer<RecordEndEvent>() {
            @Override
            public void onChanged(RecordEndEvent recordEndEvent) {
                Optional.ofNullable(recordEndEvent.getContentIfNotHandled()).ifPresent(content -> {
                    Toast.makeText(getContext(), content, Toast.LENGTH_SHORT).show();
                });
            }
        });

        oMapActivitySharedViewModel.getToolbarVisibility().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if(oToolbar != null){
                    oToolbar.setVisibility(integer);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            DeleteConfirmationDialog dialog = new DeleteConfirmationDialog();
            dialog.setCallback(oDeleteDialogCallback);
            dialog.show(getParentFragmentManager(), "deleteConfirmationDialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {
        inflater.inflate(R.menu.action_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void setButtonClickListeners(){
        oStartRecordingButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        RecordState state = oMapActivitySharedViewModel.getRecordState().getValue();
                        switch (state){
                            case RECORDING:
                                oMapActivitySharedViewModel.getRecordState().setValue(RecordState.STOP);
                                oMapActivitySharedViewModel.getRecordEndEvent().setValue(new RecordEndEvent(getString(R.string.touringFinishToast)));
                                if(oRecordServiceBound) {
                                    oRecordService.stopRecording();
                                }
                                break;
                            case STOP:
                                oMapActivitySharedViewModel.getRecordStartEvent().setValue(new RecordStartEvent(getString(R.string.touringStartToast)));
                                startRecordService();

                                cExecutors.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        while(!oRecordServiceBound){
                                            try{
                                                Thread.sleep(10);
                                            }catch(InterruptedException e){
                                                e.printStackTrace();
                                            }
                                        }
                                        Log.i(LoggerTag.SYSTEM_PROCESS, "RecordService connected, start Recording");

                                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                oMapActivitySharedViewModel.getRecordState().setValue(RecordState.RECORDING);
                                                oRecordService.startRecording();
                                            }
                                        });
                                    }
                                });
                                break;
                            default:
                                Log.e(LoggerTag.SYSTEM_PROCESS, "unexpected record state detected:" + state.getId());
                        }
                    }
                }
        );

        oMemoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MutableLiveData<Boolean> isRecordsViewVisible = oMapActivitySharedViewModel.getIsRecordsViewVisible();
                if(isRecordsViewVisible.getValue()) {
                    isRecordsViewVisible.setValue(false);
                    oMapActivitySharedViewModel.getToolbarVisibility().setValue(View.GONE);
                }else{
                    isRecordsViewVisible.setValue(true);
                }
            }
        });

    }

    private void startRecordService(){
        Intent i = new Intent(getContext(), RecordService.class);
        Activity activity = getActivity();
        activity.startForegroundService(i);
        activity.bindService(i, oRecordServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection oRecordServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            RecordService.RecordServiceBinder binder = (RecordService.RecordServiceBinder)iBinder;
            oRecordService = binder.getRecordService();
            oRecordServiceBound = true;
            oMapActivitySharedViewModel.getRecordState().setValue(oRecordService.getRecordState());
            oRecordService.setIRecordServiceCallback(oBoundMapFragment);
            oRecordService.setUnbindRequestCallback(new RecordService.IStopRequestCallback() {
                @Override
                public void onStopRecordService() {
                    oMapActivitySharedViewModel.getRecordState().setValue(RecordState.STOP);
                    unbindServiceGracefully();
                }
            });
            if(oMapActivitySharedViewModel.isRecording()){
                int recordId = oRecordService.getRecordObject().getRecordId();
                oBoundMapFragment.restorePolyline(recordId);
                oBoundMapFragment.moveCameraToLastLocation(recordId);
            }
            Log.d(LoggerTag.SYSTEM_PROCESS, "onServiceConnected MapActivity");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            oRecordServiceBound = false;
            Log.d(LoggerTag.SYSTEM_PROCESS, "onServiceDisconnected MapActivity");
        }
    };

    private void unbindServiceGracefully(){
        if(oRecordServiceBound) {
            oRecordService.setUnbindRequestCallback(null);
            getActivity().unbindService(oRecordServiceConnection);
            oRecordServiceBound = false;
            Log.d(LoggerTag.SYSTEM_PROCESS, "unbind RecordService");
        }
    }

    private void checkPermissions(){
        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }

    private void stopServiceIfNotRecording(){
        if(oMapActivitySharedViewModel.isRecording())
            return;

        if(oRecordServiceBound){
            oRecordService.stopService();
        }
    }


}
