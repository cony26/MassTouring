package com.example.masstouring.database;

public enum RecordsStartInfo implements IColumn{
    ID("id", "INTEGER", 0),
    START_TIME("start_time", "TEXT", 1);

    String oName;
    String oType;
    int oIndex;
    RecordsStartInfo(String aName, String aType, int aIndex){
        oName = aName;
        oType = aType;
        oIndex = aIndex;
    }

    @Override
    public String getName() {
        return oName;
    }

    @Override
    public String getType(){
        return oType;
    }

    @Override
    public int getIndex() {
        return oIndex;
    }
}
