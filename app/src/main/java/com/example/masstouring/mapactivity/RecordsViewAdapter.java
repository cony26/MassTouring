package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masstouring.R;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.viewmodel.MapActivtySharedViewModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RecordsViewAdapter extends RecyclerView.Adapter<RecordsViewHolder>{
    private final MapActivtySharedViewModel viewModel;
    private final IItemClickCallback callback;
    private final int initialColor;
    private final StringBuilder reuseBuilder = new StringBuilder();

    public RecordsViewAdapter(MapActivtySharedViewModel aViewModel, IItemClickCallback aCallback, Context aContext){
        viewModel = aViewModel;
        callback = aCallback;
        initialColor = ContextCompat.getColor(aContext, R.color.cardview);
    }

    @Override
    public RecordsViewHolder onCreateViewHolder(ViewGroup aParent, int aViewType){
        View v = LayoutInflater.from(aParent.getContext()).inflate(R.layout.recordsview_item, aParent, false);
        return new RecordsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecordsViewHolder aHolder, int aPosition){
        Log.d(LoggerTag.RECORD_RECYCLER_VIEW, "onBindViewHolder");

        RecordItem recordItem = viewModel.getRecordItems(false).get(aPosition);
        if(!recordItem.hasAllData()){
            viewModel.loadRecordAsync(recordItem.getId(), new RecordItemLoadRequest(aPosition));
        }

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

        reuseBuilder.setLength(0);
        BigDecimal distance = BigDecimal.valueOf(recordItem.getDistance() / 1000);
        reuseBuilder.append(distance.setScale(3, BigDecimal.ROUND_UP)).append(Const.KM_UNIT);
        aHolder.oDistanceText.setText(reuseBuilder.toString());

        String appendixText = Integer.toString(recordItem.getId());
        aHolder.oAppendixText.setText(appendixText);

        int color = initialColor;
        if(recordItem.isRendered()){
            color = Color.GRAY;
        }
        if(recordItem.isSelected()){
            color = Color.CYAN;
        }
        aHolder.itemView.setBackgroundColor(color);

        aHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recordItem == RecordItem.EMPTY_RECORD){
                    return;
                }

                int color = recordItem.isRendered() ? initialColor : Color.GRAY;
                view.setBackgroundColor(color);
                callback.onRecordItemClick(recordItem);
                recordItem.setRendered(!recordItem.isRendered());
                Log.i(LoggerTag.RECORD_RECYCLER_VIEW, "record item clicked : " + recordItem.toString());
            }
        });
        aHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(recordItem == RecordItem.EMPTY_RECORD){
                    return true;
                }

                boolean originalState = recordItem.isSelected();
                recordItem.setSelected(!originalState);
                view.setBackgroundColor(originalState ? initialColor : Color.CYAN);
                callback.onRecordItemLongClick();
                return true;
            }
        });
    }

    @Override
    public int getItemCount(){
        return viewModel.getRecordItems(false).size();
    }

    private class RecordItemLoadRequest implements MapActivtySharedViewModel.IRecordItemOperationCallback {
        private final int position;
        private RecordItemLoadRequest(int aPosition){
            position = aPosition;
        }

        @Override
        public void onCompleting() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    notifyItemChanged(position);
                }
            });
        }
    }
}
