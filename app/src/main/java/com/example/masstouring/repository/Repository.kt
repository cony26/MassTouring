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
    val cachedRecordItems: MutableList<RecordItem> = mutableListOf()

    fun restorePolylineOptionsFrom(aId: Int): PolylineOptions?{
        return db.restorePolylineOptionsFrom(aId)
    }

    fun getLastLatLngFrom(aId: Int): LatLng?{
        return db.getLastLatLngFrom(aId)
    }

    fun getRecords(force : Boolean): List<RecordItem>{
        if(force){
            cachedRecordItems.clear()
            cachedRecordItems.addAll(db.records)
        }else{
            if(cachedRecordItems.isEmpty()){
                cachedRecordItems.addAll(Collections.nCopies(getRecordSize(), RecordItem.EMPTY_RECORD))

                GlobalScope.launch {
                    val idList = db.recordIdList
                    val list = mutableListOf<RecordItem>()
                    for (id in idList){
                        list.add(RecordItem(id))
                    }
                    cachedRecordItems.clear()
                    cachedRecordItems.addAll(list)
                }
            }
        }

        return cachedRecordItems
    }

    fun getRecord(aId : Int): RecordItem{
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
        return db.recordSize
    }

    fun deleteRecord(aIds: IntArray){
        db.deleteRecord(aIds)
    }
}