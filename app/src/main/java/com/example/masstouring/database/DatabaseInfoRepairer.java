package com.example.masstouring.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.mapactivity.RecordObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * 1. delete records that has only few records. The threshold is defined in {@link #THRESHOLD_TOO_SMALL}.<br>
 * 2. repair database by inserting the proper row as {@link RecordsEndInfo}.<br>
 * This is for the case that {@link RecordsEndInfo} was not inserted correctly because of force System Shutdown.
 * The newly inserted row is taken from the last of {@link Positions} of the id.<br><br>
 *
 * The two operation is off course excluding the record under recording.
 */
public class DatabaseInfoRepairer implements Runnable{
    private DatabaseHelper oDatabaseHelper;
    private static final int THRESHOLD_TOO_SMALL = 1;

    public DatabaseInfoRepairer(Context aContext){
        oDatabaseHelper = new DatabaseHelper(aContext);
    }

    @Override
    public void run() {
        try{
            int recordingId = oDatabaseHelper.getRecordingInfo();
            deleteTooSmallRecordsExcluding(recordingId);
            repairEndInfoExcluding(recordingId);
        }catch(Exception e){
            Log.e(LoggerTag.DATABASE_PROCESS, "failed to repair database",e);
        }
    }

    private void deleteTooSmallRecordsExcluding(int aExcludedId){
        List<Integer> needDeleteTooSmallIdList = new ArrayList<>();

        try(SQLiteDatabase db = oDatabaseHelper.getReadableDatabase()){
            Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, null, null, null, null, null);
            while(recordsStartInfoCursor.moveToNext()) {
                int id = (int) Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.ID);

                //if recording, the id is not target of repair.
                if(id == aExcludedId){
                    continue;
                }

                Cursor positionsCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + id, null, null, null, null);
                if(positionsCursor.getCount() <= THRESHOLD_TOO_SMALL){
                    needDeleteTooSmallIdList.add(id);
                }
            }
        }

        oDatabaseHelper.deleteRecord(needDeleteTooSmallIdList.stream().mapToInt(id -> id).toArray());
    }

    private void repairEndInfoExcluding(int aExcludedId){
        List<Integer> needRepairEndInfoIdList = new ArrayList<>();

        try(SQLiteDatabase db = oDatabaseHelper.getReadableDatabase()){
            Cursor recordsStartInfoCursor = db.query(Tables.RECORDS_STARTINFO.getName(), null, null, null, null, null, null);
            while(recordsStartInfoCursor.moveToNext()){
                int id = (int)Tables.RECORDS_STARTINFO.get(recordsStartInfoCursor, RecordsStartInfo.ID);

                if(id == aExcludedId){
                    continue;
                }

                Cursor recordsEndInfoCusor = db.query(Tables.RECORDS_ENDINFO.getName(), null, RecordsEndInfo.ID.getQuatedName() + "=" + id, null, null, null, null);
                if(recordsEndInfoCusor.getCount() == 0){
                    needRepairEndInfoIdList.add(id);
                }
            }
        }

        for(int id : needRepairEndInfoIdList){
            RecordObject recordObject = RecordObject.createRecordObjectForRestore(id);

            try(SQLiteDatabase db = oDatabaseHelper.getReadableDatabase()){
                Cursor positionCursor = db.query(Tables.POSITIONS.getName(), null, Positions.ID.getQuatedName() + "=" + id, null, null, null, null);
                positionCursor.moveToLast();
                int order = (int)Tables.POSITIONS.get(positionCursor, Positions.ORDER);
                String date = (String)Tables.POSITIONS.get(positionCursor, Positions.TIMESTAMP);

                recordObject.setRecordNumber(order);
                recordObject.setEndDate(date);
            }

            oDatabaseHelper.recordEndInfo(recordObject);
            Log.i(LoggerTag.DATABASE_PROCESS, "successfully repaired ID:" + id);
        }
    }
}
