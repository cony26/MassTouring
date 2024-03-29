package com.example.masstouring.database;

public enum Positions implements IColumn{
    ID("id", "INTEGER",0),
    ORDER("order", "INTEGER",1),
    LATITUDE("latitude", "REAL",2),
    LONGITUDE("longitude", "REAL",3),
    TIMESTAMP("time", "TEXT", 4),
    SPEEDMPS("speedMps", "REAL", 5);

    public String oName;
    public String oType;
    public int oIndex;
    Positions(String aName, String aType, int aIndex){
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
