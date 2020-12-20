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
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.recordservice.RecordService;
import com.google.android.gms.maps.SupportMapFragment;

import java.util.List;

public class MapActivity extends AppCompatActivity implements DeleteConfirmationDialog.IDeleteConfirmationDialogCallback {

    private Button oStartRecordingButton;
    private Button oMemoryButton;
    private BoundRecordView oRecordsView;
    private Toolbar oToolbar;
    private OnBackPressedCallback oOnBackPressedCallback;
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, Const.DB_NAME);
    private BoundMapFragment oBoundMapFragment;
    private MapActivtySharedViewModel oMapActivitySharedViewModel;
    private RecordService oRecordService;
    boolean oRecordServiceBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onCreate MapActivity");
        setContentView(R.layout.activity_maps);
        oStartRecordingButton = findViewById(R.id.btnStartRecording);
        oMemoryButton = findViewById(R.id.btnMemory);

        oMapActivitySharedViewModel = new ViewModelProvider(this).get(MapActivtySharedViewModel.class);
        oRecordsView = new BoundRecordView(this, findViewById(R.id.recordsView), oMapActivitySharedViewModel, getApplicationContext());
        oToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(oToolbar);
        oToolbar.setVisibility(View.GONE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        oBoundMapFragment = new BoundMapFragment(this, mapFragment);

        oRecordsView.setMapFragment(oBoundMapFragment);
        oRecordsView.setToolbar(oToolbar);

        oOnBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                oMemoryButton.performClick();
                Log.d(LoggerTag.SYSTEM_PROCESS,"handleOnBackPressed");
            }
        };
        getOnBackPressedDispatcher().addCallback(oOnBackPressedCallback);

        setButtonClickListeners();
        startRecordService();
        setRecordStateIfExists();

        if(savedInstanceState != null){
            Log.d(LoggerTag.SYSTEM_PROCESS,"onRestoreSavedInstanceState");
        }

        subscribe();
    }

    private void subscribe(){
        oMapActivitySharedViewModel.getRecordState().observe(this, new Observer<RecordState>() {
            @Override
            public void onChanged(RecordState recordState) {
                oStartRecordingButton.setText(recordState.getButtonStringId());
                Toast.makeText(MapActivity.this, getText(recordState.getToastId()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onResume MapActivity");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onPause MapActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onDestroy MapActivity");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(LoggerTag.SYSTEM_PROCESS,"onSaveInstanceState MapActivity");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(LoggerTag.SYSTEM_PROCESS,"onRestoreInstanceState MapActivity");
        super.onRestoreInstanceState(savedInstanceState);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
                    oToolbar.setVisibility(View.GONE);
                    oOnBackPressedCallback.setEnabled(false);
                }else{
                    isRecordsViewVisible.setValue(true);
                    oOnBackPressedCallback.setEnabled(true);
                }
            }
        });

    }

    private List<RecordItem> loadRecords(){
        List<RecordItem> data = oDatabaseHelper.getRecords();

        for(RecordItem item : data){
            Log.d(LoggerTag.RECORDS, item.toString());
        }

        return data;
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
        oToolbar.setVisibility(View.GONE);
    }

    @Override
    public void onNegativeClick() {

    }
}