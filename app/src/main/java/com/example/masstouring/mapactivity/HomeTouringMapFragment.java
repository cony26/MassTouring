package com.example.masstouring.mapactivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.masstouring.R;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LifeCycleLogger;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.common.LoggerTask;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.database.DatabaseInfoRepairer;
import com.example.masstouring.mapactivity.presenter.CheckRecordsButtonPresenter;
import com.example.masstouring.mapactivity.presenter.RecordButtonPresenter;
import com.google.android.gms.maps.SupportMapFragment;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeTouringMapFragment extends Fragment {
    static final ExecutorService cExecutors = Executors.newFixedThreadPool(5);
    private BoundRecordView oBoundRecordsView;
    private BoundMapFragment oBoundMapFragment;
    private MapActivtySharedViewModel oMapActivitySharedViewModel;
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
        new LifeCycleLogger(this, getClass().getSimpleName());
        BackPressedCallbackRegisterer.initialize((AppCompatActivity)getActivity());
        checkPermissions();
        oMapActivitySharedViewModel = new ViewModelProvider(requireActivity()).get(MapActivtySharedViewModel.class);
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

        new RecordButtonPresenter(view.findViewById(R.id.btnStartRecording), this, oMapActivitySharedViewModel);
        new CheckRecordsButtonPresenter(view.findViewById(R.id.btnMemory), this, oMapActivitySharedViewModel);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        oBoundMapFragment = new BoundMapFragment(activity, oMapActivitySharedViewModel, (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.map), oDatabaseHelper);
        oBoundRecordsView = new BoundRecordView(activity, view.findViewById(R.id.recordsView), oMapActivitySharedViewModel, oDatabaseHelper);
        oBoundRecordsView.setMapFragment(oBoundMapFragment);
        subscribeLiveData();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(LoggerTag.SYSTEM_PROCESS,"onSaveInstanceState MapActivity");
        super.onSaveInstanceState(outState);
    }

    private void subscribeLiveData(){
        LifecycleOwner owner = getViewLifecycleOwner();

        oMapActivitySharedViewModel.getRecordStartEvent().observe(owner, new Observer<RecordStartEvent>() {
            @Override
            public void onChanged(RecordStartEvent recordStartEvent) {
                Optional.ofNullable(recordStartEvent.getContentIfNotHandled()).ifPresent(resId -> {
                    Toast.makeText(getContext(), getString(resId), Toast.LENGTH_SHORT).show();
                    oMapActivitySharedViewModel.getRecordServiceOrderEvent().setValue(new RecordServiceOrderEvent(RecordServiceOrderEvent.Order.START));
                    oBoundMapFragment.initialize();
                    oMapActivitySharedViewModel.isRecordsViewVisible().setValue(false);
                });
            }
        });

        oMapActivitySharedViewModel.getRecordEndEvent().observe(owner, new Observer<RecordEndEvent>() {
            @Override
            public void onChanged(RecordEndEvent recordEndEvent) {
                Optional.ofNullable(recordEndEvent.getContentIfNotHandled()).ifPresent(resId -> {
                    Toast.makeText(getContext(), getString(resId), Toast.LENGTH_SHORT).show();
                });
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

    private void checkPermissions(){
        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
}
