package com.example.masstouring.repository

import com.example.masstouring.database.DatabaseHelper
import com.example.masstouring.mapactivity.RecordItem
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class Repository @Inject constructor(
        private val db : DatabaseHelper
) {
    private val cachedRecordItems: MutableList<RecordItem> = mutableListOf()

    fun restorePolylineOptionsFrom(aId: Int): PolylineOptions?{
        return db.restorePolylineOptionsFrom(aId)
    }

    fun getLastLatLngFrom(aId: Int): LatLng?{
        return db.getLastLatLngFrom(aId)
    }

    fun cleanLoadRecordItems(): List<RecordItem>{
        cachedRecordItems.clear()
        cachedRecordItems.addAll(db.records)

        return cachedRecordItems
    }

    fun getCachedRecordItems(): List<RecordItem>{
        if(cachedRecordItems.isEmpty()){
            createRecordItemCacheWithId()
        }

        return cachedRecordItems
    }

    private fun createRecordItemCacheWithId(){
        val idList = db.recordIdList
        val list = mutableListOf<RecordItem>()
        for (id in idList){
            list.add(RecordItem(id))
        }
        cachedRecordItems.clear()
        cachedRecordItems.addAll(list)
    }

    fun loadRecord(aId : Int): RecordItem{
        if(aId == RecordItem.INVALID_ID){
            return RecordItem.EMPTY_RECORD
        }

        val recordItem = cachedRecordItems.find { it.id == aId } ?: return RecordItem.EMPTY_RECORD

        if(recordItem.hasAllData()){
            return recordItem
        }

        val loadedItem = db.getRecordItem(aId);
        val index = cachedRecordItems.indexOf(recordItem)
        cachedRecordItems.removeAt(index)
        cachedRecordItems.add(index, loadedItem)

        return loadedItem
    }

    fun getRecordSize(): Int{
        if(cachedRecordItems.isEmpty()){
            return db.recordSize
        }else{
            return cachedRecordItems.size
        }
    }

    fun deleteRecord(aIds: IntArray){
        cachedRecordItems.removeIf {
            aIds.contains(it.id)
        }
        db.deleteRecord(aIds)
    }
}