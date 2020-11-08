package com.example.masstouring.database;

public interface ITable {
    public abstract String getCreateTableSQL();
    public abstract String getDropTableSQL();
    public abstract void registerColumns();
    public abstract String getInsertSQL(Object... aObjects);
    public abstract void insertLog(Object... aObjects);
}
