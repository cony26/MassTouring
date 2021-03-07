package com.example.masstouring.mapactivity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.masstouring.R;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LifeCycleLogger;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.database.DatabaseInfoRepairer;
import com.example.masstouring.recordservice.RecordService;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapActivity extends AppCompatActivity{
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new LifeCycleLogger(this, getClass().getSimpleName());
        checkPermissions();
        setContentView(R.layout.activity_maps);
        oMapActivitySharedViewModel = new ViewModelProvider(this).get(MapActivtySharedViewModel.class);
        oDatabaseHelper = new DatabaseHelper(getApplicationContext(), Const.DB_NAME);
        oStartRecordingButton = findViewById(R.id.btnStartRecording);
        oMemoryButton = findViewById(R.id.btnMemory);
        oToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(oToolbar);
        oBoundMapFragment = new BoundMapFragment(oMapActivitySharedViewModel, (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map), oDatabaseHelper);
        oBoundRecordsView = new BoundRecordView(this, findViewById(R.id.recordsView), oMapActivitySharedViewModel, oDatabaseHelper);
        oBoundRecordsView.setMapFragment(oBoundMapFragment);

        setButtonClickListeners();
        subscribeLiveData();

        cExecutors.execute(new DatabaseInfoRepairer(oDatabaseHelper));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(LoggerTag.SYSTEM_PROCESS,"onRestoreInstanceState MapActivity");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        startRecordService();
        super.onStart();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopServiceIfNotRecording();
        unbindServiceGracefully();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(LoggerTag.SYSTEM_PROCESS,"onSaveInstanceState MapActivity");
        super.onSaveInstanceState(outState);
    }

    private void subscribeLiveData(){
        oMapActivitySharedViewModel.getRecordState().observe(this, new Observer<RecordState>() {
            @Override
            public void onChanged(RecordState recordState) {
                oStartRecordingButton.setText(recordState.getButtonStringId());
            }
        });

        oMapActivitySharedViewModel.getRecordStartEvent().observe(this, new Observer<RecordStartEvent>() {
            @Override
            public void onChanged(RecordStartEvent recordStartEvent) {
                Optional.ofNullable(recordStartEvent.getContentIfNotHandled()).ifPresent(content -> {
                    Toast.makeText(MapActivity.this, content, Toast.LENGTH_SHORT).show();
                    oBoundMapFragment.initialize();
                    oMapActivitySharedViewModel.getIsRecordsViewVisible().setValue(false);
                });
            }
        });

        oMapActivitySharedViewModel.getRecordEndEvent().observe(this, new Observer<RecordEndEvent>() {
            @Override
            public void onChanged(RecordEndEvent recordEndEvent) {
                Optional.ofNullable(recordEndEvent.getContentIfNotHandled()).ifPresent(content -> {
                    Toast.makeText(MapActivity.this, content, Toast.LENGTH_SHORT).show();
                });
            }
        });

        oMapActivitySharedViewModel.getToolbarVisibility().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                oToolbar.setVisibility(integer);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_delete:
                DeleteConfirmationDialog dialog = new DeleteConfirmationDialog();
                dialog.setCallback(oDeleteDialogCallback);
                dialog.show(getSupportFragmentManager(), "deleteConfirmationDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return super.onCreateOptionsMenu(menu);
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

                                        runOnUiThread(new Runnable() {
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
        Intent i = new Intent(MapActivity.this, RecordService.class);
        startForegroundService(i);
        bindService(i, oRecordServiceConnection, Context.BIND_AUTO_CREATE);
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
            unbindService(oRecordServiceConnection);
            oRecordServiceBound = false;
            Log.d(LoggerTag.SYSTEM_PROCESS, "unbind RecordService");
        }
    }

    private void checkPermissions(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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