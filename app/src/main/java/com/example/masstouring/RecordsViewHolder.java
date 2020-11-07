package com.example.masstouring;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class RecordsViewHolder extends RecyclerView.ViewHolder {
    TextView oDateText;
    TextView oDistanceText;
    TextView oAppendixText;

    RecordsViewHolder(View itemView){
        super(itemView);
        oDateText = itemView.findViewById(R.id.textDateText);
        oDistanceText = itemView.findViewById(R.id.totalDistanceText);
        oAppendixText = itemView.findViewById(R.id.appendixText);
    }
}
