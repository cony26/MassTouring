package com.example.masstouring;

public interface ITable {
    public abstract String getCreateTableSQL();
    public abstract String getDropTableSQL();
    public abstract void registerColumns();
    public abstract String getInsertSQL(Object... aObjects);
}
