package com.example.masstouring;

import android.app.ActionBar;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.max;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DBNAME = "MassTouring.sqlite";
    private static final int VERSION = 1;

    DatabaseHelper(Context context, String aDBName){
        super(context, aDBName, null, VERSION);
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

    public void putRecordsStartInfo(SQLiteDatabase sqLiteDatabase, int aId, String aStartTime){
        sqLiteDatabase.execSQL(Tables.RECORDS_STARTINFO.getInsertSQL(aId, aStartTime));
    }

    public void putRecordsEndInfo(SQLiteDatabase sqLiteDatabase, int aId, String aEndTime, int aOrderSize){
        sqLiteDatabase.execSQL(Tables.RECORDS_ENDINFO.getInsertSQL(aId, aEndTime, aOrderSize));
    }

    public void putPositions(SQLiteDatabase sqLiteDatabase, int aId, int aOrder, double aLat, double aLon){
        sqLiteDatabase.execSQL(Tables.POSITIONS.getInsertSQL(aId, aOrder, aLat, aLon));
    }

    public int getUniqueID() {
        Set<Integer> ids = new HashSet<>();
        int uniqueId = 0;
        try (SQLiteDatabase db = getReadableDatabase()) {
            Cursor cs = db.query(Tables.RECORDS_STARTINFO.getName(), new String[]{RecordsStartInfo.ID.getName()}, null, null, null, null, null);
            while(cs.moveToNext()) {
                ids.add(cs.getInt(0));
            }
        }catch(Exception e){
            return uniqueId;
        }
        uniqueId  = max(ids) + 1;

        return uniqueId;
    }

    public List<RecordsItem> getRecords(SQLiteDatabase sqLiteDatabase){
        List<RecordsItem> data = new ArrayList<>();
        try(SQLiteDatabase db = getReadableDatabase()){
            Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, null, null, null, null, null);
            while(recordsStartInfoCursor.moveToNext()){
                int id = (int)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.ID);
                String startInfo = (String)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.START_TIME);

                int dataSize = -1;
                String endInfo = null;
                Cursor recordsEndInfoCusor = db.query(Tables.RECORDS_ENDINFO.getName(), null, "ID="+id, null, null, null, null);
                recordsEndInfoCusor.moveToFirst();
                if(!recordsEndInfoCusor.isAfterLast()){
                    endInfo = (String)Tables.RECORDS_ENDINFO.get(recordsEndInfoCusor, RecordsEndInfo.END_TIME);
                    dataSize = (int)Tables.RECORDS_ENDINFO.get(recordsEndInfoCusor, RecordsEndInfo.ORDER_SIZE);
                }

                Map<Integer, LatLng> locationMap = new HashMap<>();
                Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, "id="+id, null, null, null, null);
                while(positionsCursor.moveToNext()){
                    double latitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.LATITUDE);
                    double altitude = (double)Tables.POSITIONS.get(positionsCursor, Positions.ALTITUDE);
                    int order = (int)Tables.POSITIONS.get(positionsCursor, Positions.ORDER);
                    locationMap.put(order, new LatLng(latitude, altitude));
                }

                data.add(new RecordsItem(id, "start:"+startInfo+",end:" + endInfo, "xxx km/h", "appendixText", locationMap));
            }
        }

        return data;
        
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
