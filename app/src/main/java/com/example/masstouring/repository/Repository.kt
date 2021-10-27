package com.example.masstouring.repository

import com.example.masstouring.database.DatabaseHelper
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import java.util.*
import javax.inject.Inject

class Repository @Inject constructor(
        private val db : DatabaseHelper
) {
    fun restorePolylineOptionsFrom(aId: Int): PolylineOptions?{
        return db.restorePolylineOptionsFrom(aId)
    }

    fun getLastLatLngFrom(aId: Int): LatLng?{
        return db.getLastLatLngFrom(aId)
    }
}