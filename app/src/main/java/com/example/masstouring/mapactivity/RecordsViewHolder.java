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

    RecordsViewHolder(View itemView){
        super(itemView);
        oYearText = itemView.findViewById(R.id.yearText);
        oStartDateText = itemView.findViewById(R.id.startDateText);
        oEndDateText = itemView.findViewById(R.id.endDateText);
        oDistanceText = itemView.findViewById(R.id.DistanceText);
        oAppendixText = itemView.findViewById(R.id.appendixText);
    }
}
