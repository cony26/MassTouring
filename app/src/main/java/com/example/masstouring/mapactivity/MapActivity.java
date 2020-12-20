package com.example.masstouring.mapactivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.masstouring.R;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LifeCycleLogger;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.recordservice.RecordService;
import com.google.android.gms.maps.SupportMapFragment;

public class MapActivity extends AppCompatActivity implements DeleteConfirmationDialog.IDeleteConfirmationDialogCallback {

    private Button oStartRecordingButton;
    private Button oMemoryButton;
    private BoundRecordView oRecordsView;
    private Toolbar oToolbar;
    private BoundMapFragment oBoundMapFragment;
    private MapActivtySharedViewModel oMapActivitySharedViewModel;
    private RecordService oRecordService;
    private boolean oRecordServiceBound = false;
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, Const.DB_NAME);
    private OnBackPressedCallback oOnBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            oMemoryButton.performClick();
            Log.d(LoggerTag.SYSTEM_PROCESS,"handleOnBackPressed");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new LifeCycleLogger(this, getClass().getSimpleName());
        setContentView(R.layout.activity_maps);
        oStartRecordingButton = findViewById(R.id.btnStartRecording);
        oMemoryButton = findViewById(R.id.btnMemory);

        oMapActivitySharedViewModel = new ViewModelProvider(this).get(MapActivtySharedViewModel.class);
        oRecordsView = new BoundRecordView(this, findViewById(R.id.recordsView), oMapActivitySharedViewModel, getApplicationContext());
        oToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(oToolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        oBoundMapFragment = new BoundMapFragment(this, mapFragment);

        oRecordsView.setMapFragment(oBoundMapFragment);

        oOnBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                oMemoryButton.performClick();
                Log.d(LoggerTag.SYSTEM_PROCESS,"handleOnBackPressed");
            }
        };
        getOnBackPressedDispatcher().addCallback(oOnBackPressedCallback);

        setButtonClickListeners();
        setRecordStateIfExists();

        if(savedInstanceState != null){
            Log.d(LoggerTag.SYSTEM_PROCESS,"onRestoreSavedInstanceState");
        }

        subscribe();
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
        unbindService(oRecordServiceConnection);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(LoggerTag.SYSTEM_PROCESS,"onSaveInstanceState MapActivity");
        super.onSaveInstanceState(outState);
    }

    private void subscribe(){
        oMapActivitySharedViewModel.getRecordState().observe(this, new Observer<RecordState>() {
            @Override
            public void onChanged(RecordState recordState) {
                oStartRecordingButton.setText(recordState.getButtonStringId());
                Toast.makeText(MapActivity.this, getText(recordState.getToastId()), Toast.LENGTH_SHORT).show();
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
                dialog.setCallback(this);
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
                                if(oRecordServiceBound) {
                                    oRecordService.stopRecording();
                                }
                                break;
                            case STOP:
                                oMapActivitySharedViewModel.getRecordState().setValue(RecordState.RECORDING);
                                startRecordService();
                                if(oRecordServiceBound){
                                    oRecordService.startRecording();
                                }
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
                    oOnBackPressedCallback.setEnabled(false);
                }else{
                    isRecordsViewVisible.setValue(true);
                    oOnBackPressedCallback.setEnabled(true);
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
            setRecordStateIfExists();
            oRecordService.setIRecordServiceCallback(oBoundMapFragment);
            Log.d(LoggerTag.SYSTEM_PROCESS, "onServiceConnected MapActivity");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            oRecordServiceBound = false;
            Log.d(LoggerTag.SYSTEM_PROCESS, "onServiceDisconnected MapActivity");
        }
    };

    private void setRecordStateIfExists(){
        if(oRecordServiceBound){
            RecordState state = oRecordService.getRecordState();
            oMapActivitySharedViewModel.getRecordState().setValue(state);
            oBoundMapFragment.moveCameraIfRecording(oRecordService, oDatabaseHelper);
            Log.d(LoggerTag.SYSTEM_PROCESS, "set RecordState from RecordService");
        }
    }

    @Override
    public void onPositiveClick() {
        oRecordsView.deleteSelectedItems();
        oMapActivitySharedViewModel.getToolbarVisibility().setValue(View.GONE);
    }

    @Override
    public void onNegativeClick() {

    }
}