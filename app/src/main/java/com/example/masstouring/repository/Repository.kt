package com.example.masstouring.repository

import com.example.masstouring.database.DatabaseHelper
import com.example.masstouring.mapactivity.RecordItem
import com.example.masstouring.mapactivity.RecordObject
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
        private val db : DatabaseHelper
) {
    private var initialized : Boolean = false
    private val cachedRecordItems: MutableList<RecordItem> = mutableListOf()
    private val lock: ReentrantLock = ReentrantLock()

    fun restorePolylineOptionsFrom(aId: Int): PolylineOptions?{
        return db.restorePolylineOptionsFrom(aId)
    }

    fun getLastLatLngFrom(aId: Int): LatLng?{
        return db.getLastLatLngFrom(aId)
    }

    fun cleanLoadRecordItems(): List<RecordItem>{
        replaceCacheRecordItems(db.records)

        return getImmutableCachedRecordItems()
    }

    fun getCachedRecordItems(): List<RecordItem>{
        lock.lock()
        try{
            if(!initialized){
                val list = loadRecordItemWithIdOnly()
                replaceCacheRecordItems(list)
                initialized = true
            }
        }finally {
            lock.unlock()
        }

        return getImmutableCachedRecordItems()
    }

    private fun loadRecordItemWithIdOnly(): List<RecordItem>{
        val idList = db.recordIdList
        val list = mutableListOf<RecordItem>()
        for (id in idList){
            list.add(RecordItem(id))
        }
        return list
    }

    fun loadRecordItem(aId : Int): RecordItem{
        if(aId == RecordItem.INVALID_ID){
            return RecordItem.EMPTY_RECORD
        }

        lock.lock()
        try{
            val recordItem = cachedRecordItems.find { it.id == aId } ?: return RecordItem.EMPTY_RECORD

            if(recordItem.hasAllData()){
                return recordItem
            }

            val loadedItem = db.getRecordItem(aId)
            val index = cachedRecordItems.indexOf(recordItem)
            cachedRecordItems.removeAt(index)
            cachedRecordItems.add(index, loadedItem)
            return loadedItem
        }finally {
            lock.unlock()
        }
    }

    fun getRecordSize(): Int{
        if(initialized){
            return cachedRecordItems.size
        }else{
            return db.recordSize
        }
    }

    fun deleteRecord(aIds: IntArray){
        lock.lock()
        try{
            cachedRecordItems.removeIf {
                aIds.contains(it.id)
            }
        }finally {
            lock.unlock()
        }

        db.deleteRecord(aIds)
    }

    private fun replaceCacheRecordItems(list: List<RecordItem>){
        lock.lock()
        try{
            cachedRecordItems.clear()
            cachedRecordItems.addAll(list)
        }finally {
            lock.unlock()
        }
    }

    private fun getImmutableCachedRecordItems(): List<RecordItem>{
        lock.lock()
        try{
            return cachedRecordItems.toList()
        }finally {
            lock.unlock()
        }
    }

    fun recordPositions(recordObj : RecordObject){
        setReloadFlag(recordObj.recordId)
        db.recordPositions(recordObj)
    }

    fun recordStartInfo(recordObj: RecordObject){
        lock.lock()
        try{
            cachedRecordItems.add(RecordItem(recordObj.recordId))
        }finally {
            lock.unlock()
        }
        db.recordStartInfo(recordObj)
    }

    fun recordEndInfo(recordObj: RecordObject){
        setReloadFlag(recordObj.recordId)
        db.recordEndInfo(recordObj)
    }

    private fun setReloadFlag(id : Int){
        lock.lock()
        try{
            cachedRecordItems.find { it.id == id }?.setReloadFlag()
        }finally {
            lock.unlock()
        }
    }

    fun setRecordingInfo(recordObj: RecordObject){
        db.setRecordingInfo(recordObj)
    }

    fun getRecordingInfo():Int{
        return db.recordingInfo
    }

    fun resetRecordingInfo():Boolean{
        return db.resetRecordingInfo()
    }

    fun restoreRecordObjectFromId(id: Int): RecordObject{
        return db.restoreRecordObjectFromId(id)
    }

    fun getUniqueId(): Int{
        return db.uniqueID
    }
}