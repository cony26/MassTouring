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
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        RecordItem recordItem = oData.get(aPosition);
        LocalDateTime startDate = recordItem.getStartDate();
        LocalDateTime endDate = recordItem.getEndDate();

        String yearText = Integer.toString(startDate.getYear());
        aHolder.oYearText.setText(yearText);

        String startDateText = startDate.format(Const.START_DATE_FORMAT);
        aHolder.oStartDateText.setText(startDateText);

        String endDateText;
        if(endDate == null){
            endDateText =  Const.NO_INFO;
        }else{
            DateTimeFormatter format;
            if (startDate.getDayOfMonth() == endDate.getDayOfMonth()) {
                format = Const.END_SAME_DATE_FORMAT;
            } else {
                format = Const.END_DIFF_DATE_FORMAT;
            }
            endDateText =  "-" + endDate.format(format);
        }
        aHolder.oEndDateText.setText(endDateText);

        StringBuilder builder = new StringBuilder();
        BigDecimal distance = new BigDecimal(recordItem.getDistance() / 1000);
        builder.append(distance.setScale(3, BigDecimal.ROUND_UP)).append(Const.KM_UNIT);
        aHolder.oDistanceText.setText(builder.toString());

        String appendixText = Integer.toString(recordItem.getId());
        aHolder.oAppendixText.setText(appendixText);

        aHolder.itemView.setBackgroundColor(recordItem.isSelected() ? Color.CYAN : oInitialColor);
        aHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oCallback.onRecordItemClick(recordItem);
                Log.i(LoggerTag.RECORD_RECYCLER_VIEW, "record item clicked : " + recordItem.toString());
            }
        });
        aHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                int color = Color.CYAN;
                recordItem.setSelected(true);
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
