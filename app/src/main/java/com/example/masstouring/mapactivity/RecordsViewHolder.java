package com.example.masstouring.mapactivity;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.masstouring.R;

public class RecordsViewHolder extends RecyclerView.ViewHolder {
    TextView oStartDateText;
    TextView oEndDateText;
    TextView oYearText;
    TextView oDistanceText;
    TextView oAppendixText;

    RecordsViewHolder(View oItemView){
        super(oItemView);
        oYearText = oItemView.findViewById(R.id.yearText);
        oStartDateText = oItemView.findViewById(R.id.startDateText);
        oEndDateText = oItemView.findViewById(R.id.endDateText);
        oDistanceText = oItemView.findViewById(R.id.DistanceText);
        oAppendixText = oItemView.findViewById(R.id.appendixText);
    }
}
