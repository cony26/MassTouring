package com.example.masstouring.ranking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.masstouring.R

class RankingViewAdapter(private val data : List<RankingCardEntity>) : RecyclerView.Adapter<RankingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.ranking_cardview, parent, false)
        return RankingViewHolder(v)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val card = data[position]
        val bar = holder.distanceProgress
        val userNameTextView = holder.userNameTextView

        bar.setProgress(card.distance.toInt(), false)
        userNameTextView.text = card.userName

    }

    override fun getItemCount(): Int {
        return data.size
    }
}