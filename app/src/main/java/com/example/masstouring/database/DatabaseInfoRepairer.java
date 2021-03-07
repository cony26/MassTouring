package com.example.masstouring.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.mapactivity.RecordObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Repair database by inserting the proper row as {@link RecordsEndInfo}.<br>
 * This is for the case that {@link RecordsEndInfo} was not inserted correctly because of force System Shutdown.
 * The newly inserted row is taken from the last of {@link Positions} of the id.
 */
public class DatabaseInfoRepairer implements Runnable{
    private DatabaseHelper oDatabaseHelper;

    public DatabaseInfoRepairer(DatabaseHelper aDatabaseHelper){
        oDatabaseHelper = aDatabaseHelper;
    }

    @Override
    public void run() {
        try{
            List<Integer> needRepairIdList = new ArrayList<>();

            try(SQLiteDatabase db = oDatabaseHelper.getReadableDatabase()){
                Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, null, null, null, null, null);
                while(recordsStartInfoCursor.moveToNext()){
                    if(recordsStartInfoCursor.isLast()){
                        //TODO:This is temporary implementation for avoiding the overwrite of the recording endinfo
                        break;
                    }
                    int id = (int)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.ID);
                    Cursor recordsEndInfoCusor = db.query(Tables.RECORDS_ENDINFO.getName(), null, RecordsEndInfo.ID.getName() + "=" + id, null, null, null, null);
                    if(recordsEndInfoCusor.getCount() == 0){
                        needRepairIdList.add(id);
                    }
                }
            }

            Log.i(LoggerTag.DATABASE_PROCESS, "size of needRepairIdList:" + needRepairIdList.size());

            for(int id : needRepairIdList){
                RecordObject recordObject = RecordObject.createRecordObjectForRestore(id);

                try(SQLiteDatabase db = oDatabaseHelper.getReadableDatabase()){
                    Cursor positionCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getName() + "=" + id, null, null, null, null);
                    positionCursor.moveToLast();
                    int order = (int)Tables.POSITIONS.get(positionCursor, Positions.ORDER);
                    String date = (String)Tables.POSITIONS.get(positionCursor, Positions.TIMESTAMP);

                    recordObject.setRecordNumber(order);
                    recordObject.setEndDate(date);
                }

                oDatabaseHelper.recordEndInfo(recordObject);
            }
        }catch(Exception e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to repair database:{}",e);
        }
    }
}
