package com.example.masstouring.mapactivity;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masstouring.R;
import com.example.masstouring.common.LoggerTag;

import java.util.ArrayList;
import java.util.List;

public class RecordsViewAdapter extends RecyclerView.Adapter<RecordsViewHolder> {
    private List<RecordItem> oData;
    private IItemClickCallback oCallback;
    private final Context oContext;
    private final List<Integer> oColorList = new ArrayList<>();
    private final int oInitialColor;

    public RecordsViewAdapter(List<RecordItem> aData, IItemClickCallback aCallback, Context aContext){
        oData = aData;
        oCallback = aCallback;
        oContext = aContext;
        oInitialColor = ContextCompat.getColor(oContext, R.color.cardview);
        for(int i = 0; i < oData.size(); i++){
            oColorList.add(oInitialColor);
        }
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
        aHolder.itemView.setBackgroundColor(oColorList.get(aPosition));
        aHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oCallback.onRecordItemClick(oData.get(aPosition).getLocationMap());
                Log.d(LoggerTag.RECORD_RECYCLER_VIEW, aHolder.itemView.toString());
            }
        });
        aHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int color = Color.CYAN;
                oColorList.set(aPosition, color);
                view.setBackgroundColor(color);
                return false;
            }
        });
        Log.d(LoggerTag.RECORD_RECYCLER_VIEW, "position:" + aPosition + ", AdapterPosition:" + aHolder.getAdapterPosition());
    }

    @Override
    public int getItemCount(){
        return oData.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecordsViewHolder aHolder) {
        Log.d(LoggerTag.RECORD_RECYCLER_VIEW, "onViewRecycled");
        aHolder.itemView.setBackgroundColor(oInitialColor);
        super.onViewRecycled(aHolder);
    }
}
