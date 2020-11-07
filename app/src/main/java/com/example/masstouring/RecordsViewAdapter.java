package com.example.masstouring;

import android.app.LauncherActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecordsViewAdapter extends RecyclerView.Adapter<RecordsViewHolder> {
    private List<RecordsItem> oData;
    private IItemClickCallback oCallback;

    public RecordsViewAdapter(List<RecordsItem> aData, IItemClickCallback aCallback){
        oData = aData;
        oCallback = aCallback;
    }

    @Override
    public RecordsViewHolder onCreateViewHolder(ViewGroup aParent, int aViewType){
        View v = LayoutInflater.from(aParent.getContext()).inflate(R.layout.recordsview_item, aParent, false);
        return new RecordsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecordsViewHolder aHolder, int aPosition){
        aHolder.oDateText.setText(oData.get(aPosition).getDateText());
        aHolder.oDistanceText.setText(oData.get(aPosition).getDistanceText());
        aHolder.oAppendixText.setText(oData.get(aPosition).getAppendixText());
        aHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oCallback.onClick(oData.get(aPosition).getLocationMap());
            }
        });
    }

    @Override
    public int getItemCount(){
        return oData.size();
    }


}
