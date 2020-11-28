package com.example.masstouring;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecordsViewAdapter extends RecyclerView.Adapter<RecordsViewHolder> {
    private List<RecordItem> oData;
    private IItemClickCallback oCallback;

    public RecordsViewAdapter(List<RecordItem> aData, IItemClickCallback aCallback){
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
        aHolder.oYearText.setText(oData.get(aPosition).getYearText());
        aHolder.oStartDateText.setText(oData.get(aPosition).getStartDateText());
        aHolder.oEndDateText.setText(oData.get(aPosition).getEndDateText());
        aHolder.oDistanceText.setText(oData.get(aPosition).getDistanceText());
        aHolder.oAppendixText.setText(oData.get(aPosition).getAppendixText());
        aHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oCallback.onRecordItemClick(oData.get(aPosition).getLocationMap());
            }
        });
    }

    @Override
    public int getItemCount(){
        return oData.size();
    }


}
