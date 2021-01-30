package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masstouring.R;
import com.example.masstouring.common.LoggerTag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecordsViewAdapter extends RecyclerView.Adapter<RecordsViewHolder> {
    private List<RecordItem> oData;
    private final IItemClickCallback oCallback;
    private final int oInitialColor;

    public RecordsViewAdapter(List<RecordItem> aData, IItemClickCallback aCallback, Context aContext){
        oData = aData;
        oCallback = aCallback;
        oInitialColor = ContextCompat.getColor(aContext, R.color.cardview);
    }

    @Override
    public RecordsViewHolder onCreateViewHolder(ViewGroup aParent, int aViewType){
        View v = LayoutInflater.from(aParent.getContext()).inflate(R.layout.recordsview_item, aParent, false);
        return new RecordsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecordsViewHolder aHolder, int aPosition){
        Log.d(LoggerTag.RECORD_RECYCLER_VIEW, "onBindViewHolder");
        aHolder.oYearText.setText(oData.get(aPosition).getYearText());
        aHolder.oStartDateText.setText(oData.get(aPosition).getStartDateText());
        aHolder.oEndDateText.setText(oData.get(aPosition).getEndDateText());
        aHolder.oDistanceText.setText(oData.get(aPosition).getDistanceText());
        aHolder.oAppendixText.setText(oData.get(aPosition).getAppendixText());
        aHolder.itemView.setBackgroundColor(oData.get(aPosition).isSelected() ? Color.CYAN : oInitialColor);
        aHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oCallback.onRecordItemClick(oData.get(aPosition).getLocationMap(), oData.get(aPosition).getSpeedkmphMap());
//                Log.d(LoggerTag.RECORD_RECYCLER_VIEW, aHolder.itemView.toString());
            }
        });
        aHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int color = Color.CYAN;
                oData.get(aPosition).setSelected(true);
                view.setBackgroundColor(color);
                oCallback.onRecordItemLongClick();
                return true;
            }
        });
//        Log.d(LoggerTag.RECORD_RECYCLER_VIEW, "position:" + aPosition + ", AdapterPosition:" + aHolder.getAdapterPosition());
    }

    @Override
    public int getItemCount(){
        return oData.size();
    }

    public List<Integer> getSelectedItemIdList(){
        return oData.stream()
                .filter(data -> data.isSelected())
                .map(data -> data.getId())
                .collect(Collectors.toList());
    }

    public void setData(List<RecordItem> aRecordItemList){
        oData = aRecordItemList;
    }
}
