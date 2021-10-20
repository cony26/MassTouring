package com.example.masstouring.mapactivity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.masstouring.R
import com.example.masstouring.common.LifeCycleLogger
import com.example.masstouring.common.LoggerTask
import com.example.masstouring.database.DatabaseInfoRepairer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TouringMapActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration : AppBarConfiguration
    private lateinit var recordServiceConnector: RecordServiceConnector
    val cExecutors: ExecutorService = Executors.newFixedThreadPool(5)

    private lateinit var toolbar : Toolbar

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

        toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)

        recordServiceConnector = RecordServiceConnector(viewModel, this)
        subscribeLiveData()

        cExecutors.execute(DatabaseInfoRepairer(applicationContext))
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
        viewModel.deleteRecordsIconVisible.observe(this, Observer {
            toolbar?.menu?.findItem(R.id.action_delete)?.isVisible = it
        })

    }
}