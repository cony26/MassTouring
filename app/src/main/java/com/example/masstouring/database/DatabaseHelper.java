package com.example.masstouring.database;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.mapactivity.RecordItem;
import com.example.masstouring.mapactivity.RecordObject;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

import static java.util.Collections.max;

@Singleton
public class DatabaseHelper extends SQLiteOpenHelper {
    @Inject
    public DatabaseHelper(@ApplicationContext Context context){
        super(context, Const.DB_NAME, null, Const.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        for(Tables table : Tables.values()){
            sqLiteDatabase.execSQL(table.getCreateTableSQL());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        for(Tables table : Tables.values()){
            sqLiteDatabase.execSQL(table.getDropTableSQL());
        }
        onCreate(sqLiteDatabase);
    }

    @Override
    public void onOpen(SQLiteDatabase sqLiteDatabase){
        super.onOpen(sqLiteDatabase);
    }

    private void putRecordsStartInfo(SQLiteDatabase sqLiteDatabase, int aId, String aStartTime){
        sqLiteDatabase.execSQL(Tables.RECORDS_STARTINFO.getInsertSQL(aId, aStartTime));
        Tables.RECORDS_STARTINFO.insertLog(aId, aStartTime);
    }

    public void recordStartInfo(RecordObject aObj){
        try(SQLiteDatabase db = getWritableDatabase()){
            putRecordsStartInfo(db, aObj.getRecordId(), aObj.getStartDate());
        }catch(SQLException | CursorIndexOutOfBoundsException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to recording recordStartInfo from RecordObject:" + aObj, e);
        }
    }

    private void putRecordsEndInfo(SQLiteDatabase sqLiteDatabase, int aId, String aEndTime, int aOrderSize){
        sqLiteDatabase.execSQL(Tables.RECORDS_ENDINFO.getInsertSQL(aId, aEndTime, aOrderSize));
        Tables.RECORDS_ENDINFO.insertLog(aId, aEndTime, aOrderSize);
    }

    public void recordEndInfo(RecordObject aObj){
        try(SQLiteDatabase db = getWritableDatabase()){
            putRecordsEndInfo(db, aObj.getRecordId(), aObj.getEndDate(), aObj.getRecordNumber());
        }catch(SQLException | CursorIndexOutOfBoundsException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to recording recordEndInfo from RecordObject:" + aObj, e);
        }
    }

    public boolean setRecordingInfo(RecordObject aObj){
        try(SQLiteDatabase db = getWritableDatabase()){
            int id = aObj.getRecordId();
            db.execSQL(Tables.RECORDING_INFO.getInsertSQL(id));
            Tables.RECORDING_INFO.insertLog(id);
            Log.d(LoggerTag.DATABASE_PROCESS, "record recording info");
            return true;
        }catch(SQLException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to record recording info");
            return false;
        }
    }

    public boolean resetRecordingInfo(){
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.delete(Tables.RECORDING_INFO.getName(), "1" /* delete all*/, null);
            Log.d(LoggerTag.DATABASE_PROCESS, "reset recording info");
            return true;
        }catch(SQLException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to reset recording info", e);
            return false;
        }
    }

    public int getRecordingInfo(){
        try (SQLiteDatabase db = getReadableDatabase()) {
            try(Cursor recordingInfoCursor = db.query(Tables.RECORDING_INFO.getName(), null, null, null, null, null, null)){
                if(recordingInfoCursor.getCount() == 0){
                    return Const.INVALID_ID;
                }else{
                    recordingInfoCursor.moveToNext();
                    return (int)Tables.RECORDING_INFO.get(recordingInfoCursor, RecordingInfo.ID);
                }
            }
        }catch(SQLException | CursorIndexOutOfBoundsException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "error on getting Recording Info", e);
            return Const.INVALID_ID;
        }
    }

    private void putPositions(SQLiteDatabase sqLiteDatabase, int aId, int aOrder, double aLat, double aLon, String aDate, double aSpeedMps){
        sqLiteDatabase.execSQL(Tables.POSITIONS.getInsertSQL(aId, aOrder, aLat, aLon, aDate, aSpeedMps));
        Tables.POSITIONS.insertLog(aId, aOrder, aLat, aLon, aDate, aSpeedMps);
    }

    public void recordPositions(RecordObject aObj){
        try (SQLiteDatabase db = getWritableDatabase()) {
            Location loc = aObj.getLastLocation();
            putPositions(db, aObj.getRecordId(), aObj.getRecordNumber(), loc.getLatitude(), loc.getLongitude(), LocalDateTime.now().format(Const.DATE_FORMAT), loc.getSpeed());
        }catch(SQLException | CursorIndexOutOfBoundsException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to recording Positions from RecordObject:" + aObj, e);
        }
    }

    public void deleteRecord(int[] aIds){
        try (SQLiteDatabase db = getWritableDatabase()) {
            for(int id : aIds){
                db.delete(Tables.RECORDS_STARTINFO.getName(), RecordsStartInfo.ID.getQuatedName() + "=" + id, null);
                db.delete(Tables.POSITIONS.getName(), Positions.ID.getQuatedName() + "=" + id, null);
                db.delete(Tables.RECORDS_ENDINFO.getName(), RecordsEndInfo.ID.getQuatedName() + "=" + id, null);
                Log.d(LoggerTag.DATABASE_PROCESS, "deleted successfully, ID:" + id);
            }
        }catch(SQLException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to delete ID:", e);
        }
    }

    /**
     * @return integer that is grater or equal to 0 that is maximum value of existing IDs.
     */
    public int getUniqueID() {
        Set<Integer> ids = new HashSet<>();
        int uniqueId = 0;
        try (SQLiteDatabase db = getReadableDatabase()) {
            try(Cursor cs = db.query(Tables.RECORDS_STARTINFO.getName(), new String[]{RecordsStartInfo.ID.getQuatedName()}, null, null, null, null, null)){
                while(cs.moveToNext()) {
                    ids.add(cs.getInt(0));
                }
            }
        }catch(Exception e){
            return uniqueId;
        }
        if(ids.size() != 0){
            uniqueId  = max(ids) + 1;
        }

        return uniqueId;
    }

    public int getRecordSize(){
        try(SQLiteDatabase db = getReadableDatabase()){
            try(Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, null, null, null, null, null)){
                return recordsStartInfoCursor.getCount();
            }
        }catch(SQLException | CursorIndexOutOfBoundsException | IllegalStateException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to loading RecordSize:", e);
            return 0;
        }
    }

    public List<Integer> getRecordIdList(){
        List<Integer> recordIdList = new ArrayList<>();
        try(SQLiteDatabase db = getReadableDatabase()){
            try(Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, null, null, null, null, null)){
                while(recordsStartInfoCursor.moveToNext()){
                    int id = (int)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.ID);
                    recordIdList.add(id);
                }
            }
        }catch(SQLException | CursorIndexOutOfBoundsException | IllegalStateException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to load RecordIdList:", e);
        }
        return recordIdList;
    }

    public List<RecordItem> getRecords(){
        List<RecordItem> data = new ArrayList<>();
        try(SQLiteDatabase db = getReadableDatabase()){
            try(Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, null, null, null, null, null)){
                while(recordsStartInfoCursor.moveToNext()){
                    int id = (int)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.ID);
                    String startInfo = (String)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.START_TIME);

                    String endInfo = Const.NO_INFO;
                    try(Cursor recordsEndInfoCusor = db.query(Tables.RECORDS_ENDINFO.getName(), null, RecordsEndInfo.ID.getQuatedName() + "=" + id, null, null, null, null)){
                        recordsEndInfoCusor.moveToNext();
                        if(!recordsEndInfoCusor.isAfterLast()){
                            endInfo = (String)Tables.RECORDS_ENDINFO.get(recordsEndInfoCusor, RecordsEndInfo.END_TIME);
                        }
                    }

                    Map<Integer, LatLng> locationMap = new HashMap<>();
                    Map<Integer, String> timeStampMap = new HashMap<>();
                    Map<Integer, Double> speedkmph = new HashMap<>();
                    try(Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + id, null, null, null, null)){
                        while(positionsCursor.moveToNext()){
                            double latitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LATITUDE);
                            double longitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LONGITUDE);
                            int order = (int)Tables.POSITIONS.get(positionsCursor, Positions.ORDER);
                            String date = (String)Tables.POSITIONS.get(positionsCursor, Positions.TIMESTAMP);
                            locationMap.put(order, new LatLng(latitude, longitude));
                            timeStampMap.put(order, date);
                            speedkmph.put(order, (double)Tables.POSITIONS.get(positionsCursor, Positions.SPEEDMPS) * 60 * 60 / 1000);
                        }
                    }

                    StringBuilder builder = new StringBuilder();
                    builder.append("id:").append(id).append(", startDate:").append(startInfo).append(", endDate:").append(endInfo);
                    data.add(new RecordItem(id, startInfo, endInfo, locationMap, timeStampMap, speedkmph));
                }
            }
        }catch(SQLException | CursorIndexOutOfBoundsException | IllegalStateException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to loading Records:", e);
        }

        for(RecordItem item : data){
            Log.d(LoggerTag.DATABASE_PROCESS, "loaded:" + item.toString());
        }

        return data;
    }

    public RecordItem getRecordItem(int aId){
        RecordItem recordItem = null;
        try(SQLiteDatabase db = getReadableDatabase()){
            try(Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, RecordsEndInfo.ID.getQuatedName() + "=" + aId, null, null, null, null);
                Cursor recordsEndInfoCusor = db.query(Tables.RECORDS_ENDINFO.getName(), null, RecordsEndInfo.ID.getQuatedName() + "=" + aId, null, null, null, null);
                Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + aId, null, null, null, null)){
                recordsStartInfoCursor.moveToNext();
                String startInfo = (String)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.START_TIME);

                String endInfo = Const.NO_INFO;
                recordsEndInfoCusor.moveToNext();
                if(!recordsEndInfoCusor.isAfterLast()){
                    endInfo = (String)Tables.RECORDS_ENDINFO.get(recordsEndInfoCusor, RecordsEndInfo.END_TIME);
                }

                Map<Integer, LatLng> locationMap = new HashMap<>();
                Map<Integer, String> timeStampMap = new HashMap<>();
                Map<Integer, Double> speedkmph = new HashMap<>();

                while(positionsCursor.moveToNext()){
                    double latitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LATITUDE);
                    double longitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LONGITUDE);
                    int order = (int)Tables.POSITIONS.get(positionsCursor, Positions.ORDER);
                    String date = (String)Tables.POSITIONS.get(positionsCursor, Positions.TIMESTAMP);
                    locationMap.put(order, new LatLng(latitude, longitude));
                    timeStampMap.put(order, date);
                    speedkmph.put(order, (double)Tables.POSITIONS.get(positionsCursor, Positions.SPEEDMPS) * 60 * 60 / 1000);
                }

                recordItem = new RecordItem(aId, startInfo, endInfo, locationMap, timeStampMap, speedkmph);
                Log.d(LoggerTag.DATABASE_PROCESS, "loaded:" + recordItem.toString());
            }
        }catch(SQLException | CursorIndexOutOfBoundsException | IllegalStateException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to load RecordItem[id:" + aId + "]", e);
        }

        return recordItem;
    }

    public RecordObject restoreRecordObjectFromId(int aId){
        RecordObject recordObject = new RecordObject(aId);
        try(SQLiteDatabase db = getReadableDatabase()){
            try(Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, RecordsEndInfo.ID.getQuatedName() + "=" + aId, null, null, null, null);
                Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + aId, null, null, null, null)){
                recordsStartInfoCursor.moveToNext();
                String startInfo = (String)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.START_TIME);
                recordObject.setStartDate(startInfo);

                if(positionsCursor.getCount() == 0){
                    recordObject.setRecordNumber(-1);
                    recordObject.setLastRecordedLocation(null);
                }else{
                    positionsCursor.moveToLast();

                    int order = (int)Tables.POSITIONS.get(positionsCursor, Positions.ORDER);
                    recordObject.setRecordNumber(order);

                    double latitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LATITUDE);
                    double longitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LONGITUDE);
                    Location location = new Location("");
                    location.setLatitude(latitude);
                    location.setLongitude(longitude);
                    recordObject.setLastRecordedLocation(location);
                }
            }
        }catch(SQLException | CursorIndexOutOfBoundsException | IllegalStateException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to restoring RecordObject from ID:" + aId, e);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[RESTORED] id:").append(aId).append(", startDate:").append(recordObject.getStartDate()).append(", RecordNumber:").append(recordObject.getRecordNumber());
        Log.d(LoggerTag.SYSTEM_PROCESS, builder.toString());

        return recordObject;
    }

    public PolylineOptions restorePolylineOptionsFrom(int aId){
        PolylineOptions polylineOptions = new PolylineOptions();
        try(SQLiteDatabase db = getReadableDatabase()){
            try(Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + aId, null, null, null, null)){
                while(positionsCursor.moveToNext()){
                    double latitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LATITUDE);
                    double longitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LONGITUDE);
                    polylineOptions.add(new LatLng(latitude, longitude));
                }
            }
        }catch(SQLException | CursorIndexOutOfBoundsException | IllegalStateException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to restoring PolylineOptions from ID:" + aId, e);
        }

        Log.d(LoggerTag.DATABASE_PROCESS, "restore PolylineOptions From Id");
        return polylineOptions;
    }

    public LatLng getLastLatLngFrom(int aId){
        LatLng latLng = null;
        try(SQLiteDatabase db = getReadableDatabase()) {
            try(Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + aId, null, null, null, null)){
                positionsCursor.moveToLast();
                if(!positionsCursor.isAfterLast()) {
                    double latitude = (double) Tables.POSITIONS.get(positionsCursor, Positions.LATITUDE);
                    double longitude = (double) Tables.POSITIONS.get(positionsCursor, Positions.LONGITUDE);
                    latLng = new LatLng(latitude, longitude);
                }
            }
        }catch(SQLException | CursorIndexOutOfBoundsException | IllegalStateException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to getting last LatLng from ID:" + aId, e);
        }

        Log.d(LoggerTag.DATABASE_PROCESS, "get Last LatLng From Id");
        return latLng;
    }

    public void debugPrint(){
        try(SQLiteDatabase db = getReadableDatabase()) {
            StringBuilder builder = new StringBuilder();
            builder.append("xxxxxxx Debug_Print xxxxxx\n");
            for(Tables table : Tables.values()){
                builder.append(table.getRecords(db)).append("\n");
            }
            Log.d("test", builder.toString());
        }
    }
}
