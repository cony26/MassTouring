package com.example.masstouring.ranking

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.masstouring.R

class RankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val userNameTextView : TextView = itemView.findViewById(R.id.user_name)
    val distanceProgress : ProgressBar = itemView.findViewById(R.id.distanceProgressBar)
}