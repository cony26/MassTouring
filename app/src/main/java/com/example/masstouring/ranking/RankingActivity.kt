package com.example.masstouring.ranking

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.masstouring.R

class RankingActivity : AppCompatActivity() {
    private var recyclerView : RecyclerView? = null
    private var manager : LinearLayoutManager? = null
    private var adapter : RankingViewAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        recyclerView = findViewById<RecyclerView>(R.id.ranking_view)
        manager = LinearLayoutManager(recyclerView?.context)
        manager?.orientation = LinearLayoutManager.VERTICAL
        recyclerView?.layoutManager = manager

        val testList : MutableList<RankingCardEntity> = mutableListOf()
        testList.add(RankingCardEntity("user1", 35.353))
        testList.add(RankingCardEntity("user2", 65.353))
        repeat(20) {
            testList.add(RankingCardEntity("user${it}", it * 5.toDouble()))
        }

        adapter = RankingViewAdapter(testList)
        recyclerView?.adapter = adapter
    }
}