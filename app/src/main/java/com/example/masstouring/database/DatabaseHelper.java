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
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.max;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context, String aDBName){
        super(context, aDBName, null, Const.DB_VERSION);
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
        }
    }

    private void putRecordsEndInfo(SQLiteDatabase sqLiteDatabase, int aId, String aEndTime, int aOrderSize){
        sqLiteDatabase.execSQL(Tables.RECORDS_ENDINFO.getInsertSQL(aId, aEndTime, aOrderSize));
        Tables.RECORDS_ENDINFO.insertLog(aId, aEndTime, aOrderSize);
    }

    public void recordEndInfo(RecordObject aObj){
        try(SQLiteDatabase db = getWritableDatabase()){
            putRecordsEndInfo(db, aObj.getRecordId(), aObj.getEndDate(), aObj.getRecordNumber());
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
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to reset recording info");
            return false;
        }
    }

    public int getRecordingInfo(){
        try (SQLiteDatabase db = getReadableDatabase()) {
            Cursor recordingInfoCursor = db.query(Tables.RECORDING_INFO.getName(), null, null, null, null, null, null);
            if(recordingInfoCursor.getCount() == 0){
                return Const.INVALID_ID;
            }else{
                recordingInfoCursor.moveToNext();
                return (int)Tables.RECORDING_INFO.get(recordingInfoCursor, RecordingInfo.ID);
            }
        }catch(SQLException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "error on getting Recording Info");
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
        }
    }

    public void deleteRecord(int aId){
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.delete(Tables.RECORDS_STARTINFO.getName(), RecordsStartInfo.ID.getQuatedName() + "=" + aId, null);
            db.delete(Tables.POSITIONS.getName(), Positions.ID.getQuatedName() + "=" + aId, null);
            db.delete(Tables.RECORDS_ENDINFO.getName(), RecordsEndInfo.ID.getQuatedName() + "=" + aId, null);
            Log.d(LoggerTag.DATABASE_PROCESS, "deleted successfully, ID:" + aId);
        }catch(SQLException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to delete ID:" + aId);
        }
    }

    /**
     * @return integer that is grater or equal to 0 that is maximum value of existing IDs.
     */
    public int getUniqueID() {
        Set<Integer> ids = new HashSet<>();
        int uniqueId = 0;
        try (SQLiteDatabase db = getReadableDatabase()) {
            Cursor cs = db.query(Tables.RECORDS_STARTINFO.getName(), new String[]{RecordsStartInfo.ID.getQuatedName()}, null, null, null, null, null);
            while(cs.moveToNext()) {
                ids.add(cs.getInt(0));
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
            Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, null, null, null, null, null);
            return recordsStartInfoCursor.getCount();
        }
    }

    public List<RecordItem> getRecords(){
        List<RecordItem> data = new ArrayList<>();
        try(SQLiteDatabase db = getReadableDatabase()){
            Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, null, null, null, null, null);
            while(recordsStartInfoCursor.moveToNext()){
                int id = (int)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.ID);
                String startInfo = (String)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.START_TIME);

                String endInfo = Const.NO_INFO;
                Cursor recordsEndInfoCusor = db.query(Tables.RECORDS_ENDINFO.getName(), null, RecordsEndInfo.ID.getQuatedName() + "=" + id, null, null, null, null);
                recordsEndInfoCusor.moveToNext();
                if(!recordsEndInfoCusor.isAfterLast()){
                    endInfo = (String)Tables.RECORDS_ENDINFO.get(recordsEndInfoCusor, RecordsEndInfo.END_TIME);
                }

                Map<Integer, LatLng> locationMap = new HashMap<>();
                Map<Integer, String> timeStampMap = new HashMap<>();
                Map<Integer, Double> speedkmph = new HashMap<>();
                Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + id, null, null, null, null);
                while(positionsCursor.moveToNext()){
                    double latitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LATITUDE);
                    double longitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LONGITUDE);
                    int order = (int)Tables.POSITIONS.get(positionsCursor, Positions.ORDER);
                    String date = (String)Tables.POSITIONS.get(positionsCursor, Positions.TIMESTAMP);
                    locationMap.put(order, new LatLng(latitude, longitude));
                    timeStampMap.put(order, date);
                    speedkmph.put(order, (double)Tables.POSITIONS.get(positionsCursor, Positions.SPEEDMPS) * 60 * 60 / 1000);
                }

                StringBuilder builder = new StringBuilder();
                builder.append("id:").append(id).append(", startDate:").append(startInfo).append(", endDate:").append(endInfo);
                data.add(new RecordItem(id, startInfo, endInfo, locationMap, timeStampMap, speedkmph));
            }
        }

        for(RecordItem item : data){
            Log.d(LoggerTag.DATABASE_PROCESS, "loaded:" + item.toString());
        }

        return data;
        
    }

    public RecordObject restoreRecordObjectFromId(int aId){
        RecordObject recordObject = RecordObject.createRecordObjectForRestore(aId);
        try(SQLiteDatabase db = getReadableDatabase()){
            Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, RecordsEndInfo.ID.getQuatedName() + "=" + aId, null, null, null, null);
            recordsStartInfoCursor.moveToNext();
            String startInfo = (String)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.START_TIME);
            recordObject.setStartDate(startInfo);

            Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + aId, null, null, null, null);
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
        }catch(SQLException | CursorIndexOutOfBoundsException e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to restoring RecordObject from ID:" + aId + " " + e);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[RESTORED] id:").append(aId).append(", startDate:").append(recordObject.getStartDate()).append(", RecordNumber:").append(recordObject.getRecordNumber());
        Log.d(LoggerTag.SYSTEM_PROCESS, builder.toString());

        return recordObject;
    }

    public PolylineOptions restorePolylineOptionsFrom(int aId){
        PolylineOptions polylineOptions = new PolylineOptions();
        try(SQLiteDatabase db = getReadableDatabase()){
            Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + aId, null, null, null, null);
            while(positionsCursor.moveToNext()){
                double latitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LATITUDE);
                double longitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LONGITUDE);
                polylineOptions.add(new LatLng(latitude, longitude));
            }
        }

        Log.d(LoggerTag.DATABASE_PROCESS, "restore Polyline Option From Id");
        return polylineOptions;
    }

    public Optional<LatLng> getLastLatLngFrom(int aId){
        LatLng latLng = null;
        try(SQLiteDatabase db = getReadableDatabase()) {
            Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + aId, null, null, null, null);
            positionsCursor.moveToLast();
            if(!positionsCursor.isAfterLast()) {
                double latitude = (double) Tables.POSITIONS.get(positionsCursor, Positions.LATITUDE);
                double longitude = (double) Tables.POSITIONS.get(positionsCursor, Positions.LONGITUDE);
                latLng = new LatLng(latitude, longitude);
            }
        }

        Log.d(LoggerTag.DATABASE_PROCESS, "get Last LatLng From Id");
        return Optional.ofNullable(latLng);
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
