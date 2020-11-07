package com.example.masstouring;

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
    public String getWithType(){
        StringBuilder builder = new StringBuilder();
        builder.append("`").append(oName).append("` ").append(oType);
        return builder.toString();
    }

    @Override
    public String getName(){
        StringBuilder builder = new StringBuilder();
        builder.append("`").append(oName).append("`");
        return builder.toString();
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
