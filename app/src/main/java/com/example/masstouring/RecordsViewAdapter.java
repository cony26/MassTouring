package com.example.masstouring;

import android.app.LauncherActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecordsViewAdapter extends RecyclerView.Adapter<RecordsViewHolder> {
    private List<RecordsItem> oData;

    public RecordsViewAdapter(List<RecordsItem> aData){
        oData = aData;
    }

    @Override
    public RecordsViewHolder onCreateViewHolder(ViewGroup aParent, int aViewType){
        View v = LayoutInflater.from(aParent.getContext()).inflate(R.layout.recordsview_item, aParent, false);
        return new RecordsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecordsViewHolder aHolder, int aPosition){
        aHolder.oDateText.setText(oData.get(aPosition).getId() + "");
        aHolder.oDistanceText.setText(oData.get(aPosition).getDistanceText());
        aHolder.oAppendixText.setText(oData.get(aPosition).getAppendixText());
    }

    @Override
    public int getItemCount(){
        return oData.size();
    }
}
