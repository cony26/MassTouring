package com.example.masstouring.database;

public enum RecordsEndInfo implements IColumn{
    ID("id", "INTEGER",0),
    END_TIME("end_time", "TEXT",1),
    ORDER_SIZE("order_size", "INTEGER",2);

    String oName;
    String oType;
    int oIndex;
    RecordsEndInfo(String aName, String aType, int aIndex){
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
