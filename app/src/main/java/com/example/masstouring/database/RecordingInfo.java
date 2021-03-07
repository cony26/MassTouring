package com.example.masstouring.database;

public enum RecordingInfo implements IColumn{
    ID("id", "INTEGER",0);

    String oName;
    String oType;
    int oIndex;

    RecordingInfo(String aName, String aType, int aIndex){
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
