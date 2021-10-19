package com.example.masstouring.mapactivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.masstouring.R
import com.example.masstouring.common.LifeCycleLogger
import com.example.masstouring.common.LoggerTask

class TouringMapActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration : AppBarConfiguration
    private lateinit var boundRecordService: BoundRecordService
    private val viewModel: MapActivtySharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoggerTask.getInstance().setMapActivityState(true)
        LifeCycleLogger(this, javaClass.simpleName)

        setContentView(R.layout.touring_map_activity)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment? ?: return
        val navController = navHostFragment.navController

        val drawerLayout : DrawerLayout? = findViewById(R.id.drawer_layout)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeTouringMapFragment), drawerLayout)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)

        boundRecordService = BoundRecordService(viewModel, this)
        subscribeLiveData()
    }

    override fun onStart() {
        viewModel.recordServiceOrderEvent.value = RecordServiceOrderEvent(RecordServiceOrderEvent.Order.BOUND)
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        LoggerTask.getInstance().setMapActivityState(false)
    }

    private fun subscribeLiveData(){

    }
}