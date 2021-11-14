package com.example.masstouring.mapactivity;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.masstouring.R;
import com.example.masstouring.common.LifeCycleLogger;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.event.RecordEndEvent;
import com.example.masstouring.event.RecordServiceOrderEvent;
import com.example.masstouring.event.RecordStartEvent;
import com.example.masstouring.mapactivity.presenter.CheckRecordsButtonPresenter;
import com.example.masstouring.mapactivity.presenter.RecordButtonPresenter;
import com.example.masstouring.viewmodel.MapActivtySharedViewModel;
import com.google.android.gms.maps.SupportMapFragment;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeTouringMapFragment extends Fragment {
    private RecordViewController recordsViewController;
    private GoogleMapController googleMapController;
    private MapActivtySharedViewModel mapActivitySharedViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new LifeCycleLogger(this);
        BackPressedCallbackRegisterer.initialize((AppCompatActivity)getActivity());
        mapActivitySharedViewModel = new ViewModelProvider(requireActivity()).get(MapActivtySharedViewModel.class);
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

        new RecordButtonPresenter(view.findViewById(R.id.btnStartRecording), this, mapActivitySharedViewModel);
        new CheckRecordsButtonPresenter(view.findViewById(R.id.btnMemory), this, mapActivitySharedViewModel);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        SupportMapFragment mapFragment = (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.map);
        recordsViewController = new RecordViewController(activity);
        googleMapController = new GoogleMapController(activity, mapFragment);
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

        mapActivitySharedViewModel.getRecordStartEvent().observe(owner, new Observer<RecordStartEvent>() {
            @Override
            public void onChanged(RecordStartEvent recordStartEvent) {
                Optional.ofNullable(recordStartEvent.getContentIfNotHandled()).ifPresent(resId -> {
                    Toast.makeText(getContext(), getString(resId), Toast.LENGTH_SHORT).show();
                    mapActivitySharedViewModel.getRecordServiceOrderEvent().setValue(new RecordServiceOrderEvent(RecordServiceOrderEvent.Order.START));
                    googleMapController.initialize();
                    mapActivitySharedViewModel.isRecordsViewVisible().setValue(false);
                });
            }
        });

        mapActivitySharedViewModel.getRecordEndEvent().observe(owner, new Observer<RecordEndEvent>() {
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
            dialog.setCallback(recordsViewController.oDeleteDialogCallback);
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
}
