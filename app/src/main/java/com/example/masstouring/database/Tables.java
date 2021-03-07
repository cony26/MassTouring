package com.example.masstouring.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.masstouring.common.LoggerTag;

public enum Tables implements ITable{
    POSITIONS("positions"){
        @Override
        public void registerColumns(){
            setPrimaryKeys(new IColumn[]{Positions.ID, Positions.ORDER});
            setColumns(new IColumn[]{Positions.ID, Positions.ORDER, Positions.LATITUDE, Positions.LONGITUDE, Positions.TIMESTAMP, Positions.SPEEDMPS});
        }
    },
    RECORDS_STARTINFO("records_startinfo"){
        @Override
        public void registerColumns(){
            setPrimaryKeys(new IColumn[]{RecordsStartInfo.ID});
            setColumns(new IColumn[]{RecordsStartInfo.ID, RecordsStartInfo.START_TIME});
        }
    },
    RECORDS_ENDINFO("records_endinfo"){
        @Override
        public void registerColumns(){
            setPrimaryKeys(new IColumn[]{RecordsEndInfo.ID});
            setColumns(new IColumn[]{RecordsEndInfo.ID, RecordsEndInfo.END_TIME, RecordsEndInfo.ORDER_SIZE});
        }
    },
    RECORDING_INFO("recording_info"){
        @Override
        public void registerColumns() {
            setPrimaryKeys(new IColumn[]{RecordingInfo.ID});
            setColumns(new IColumn[]{RecordingInfo.ID});
        }
    };

    private String oName;
    private IColumn[] oPrimaryKeys;
    private IColumn[] oColumns;

    public IColumn[] getColumns() {
        return oColumns;
    }

    public String getName() {
        return oName;
    }

    void setPrimaryKeys(IColumn[] aPrimaryKeys) {
        oPrimaryKeys = aPrimaryKeys;
    }

    void setColumns(IColumn[] aColumns) {
        oColumns = aColumns;
    }

    Tables(String aName){
        oName = aName;
        registerColumns();
    }

    @Override
    public String toString(){
        return oName;
    }

    @Override
    public String getDropTableSQL(){
        StringBuilder builder = new StringBuilder();
        builder.append("DROP TABLE IF EXISTS ").append(oName);
        return builder.toString();
    }

    @Override
    public String getInsertSQL(Object... aObjects){
        StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO ").append(oName).append(" (");
        for(int i = 0; i < oColumns.length; i++){
            builder.append(oColumns[i].getQuatedName());
            if(i < oColumns.length - 1){
                builder.append(",");
            }else{
                builder.append(")");
            }
        }
        builder.append(" VALUES(");
        for(int i = 0; i < aObjects.length; i++){
            builder.append("'")
                    .append(aObjects[i].toString())
                    .append("'");
            if(i < aObjects.length - 1){
                builder.append(",");
            }else{
                builder.append(")");
            }
        }
        return builder.toString();
    }

    @Override
    public void insertLog(Object... aObjects) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for(int i = 0; i < oColumns.length; i++){
            builder.append(oColumns[i].getQuatedName())
                    .append(":")
                    .append(aObjects[i]);
            if(i < oColumns.length - 1){
                builder.append(",");
            }else{
                builder.append("]");
            }
        }
        Log.d(LoggerTag.RECORDS, builder.toString());
    }

    @Override
    public String getCreateTableSQL(){
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(oName).append(" (");
        for(IColumn column : oColumns){
            builder.append(column.getQuatedNameWithType()).append(", ");
        }
        builder.append("PRIMARY KEY (");
        for(int i = 0; i < oPrimaryKeys.length; i++){
            builder.append(oPrimaryKeys[i].getQuatedName());
            if(i < oPrimaryKeys.length - 1){
                builder.append(",");
            }else{
                builder.append(")");
            }
        }
        builder.append(")");

        return builder.toString();
    }

    public String getRecords(SQLiteDatabase db){
        StringBuilder builder = new StringBuilder();
        Cursor cs = db.query(oName, null, null, null, null, null, null);

        builder.append(oName).append("\n");
        for(int i = 0; i < oColumns.length; i++) {
            builder.append(oColumns[i].getQuatedName());
            if(i < oColumns.length - 1){
                builder.append("|");
            }else{
                builder.append("\n");
            }
        }

        while (cs.moveToNext()) {
            for(int i = 0; i < oColumns.length; i++) {
                builder.append(get(cs, oColumns[i]));
                if(i < oColumns.length - 1){
                    builder.append("|");
                }else{
                    builder.append("\n");
                }
            }

        }
        return builder.toString();
    }

    public Object get(Cursor aCursor, IColumn aColumn){
        int i = aColumn.getIndex();
        switch (aColumn.getType()){
            case "INTEGER":
                return aCursor.getInt(i);
            case "TEXT":
                return aCursor.getString(i);
            case "REAL":
                return aCursor.getDouble(i);
            default:
                return null;
        }
    }


}
