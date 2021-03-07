package com.example.masstouring.database;

public interface IColumn {
    default String getQuatedNameWithType(){
        StringBuilder builder = new StringBuilder();
        builder.append("`").append(getName()).append("` ").append(getType());
        return builder.toString();
    };

    default String getQuatedName(){
        StringBuilder builder = new StringBuilder();
        builder.append("`").append(getName()).append("`");
        return builder.toString();
    };

    public String getName();

    public String getType();

    public int getIndex();

}
