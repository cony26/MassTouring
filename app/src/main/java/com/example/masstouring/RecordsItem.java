package com.example.masstouring;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Map;

public class RecordsItem {
    private final long oId;
    private final String oDateText;
    private final String oDistanceText;
    private final String oAppendixText;
    private final Map<Integer, LatLng> oLocationMap;

    public long getId() {
        return oId;
    }

    public String getDateText() {
        return oDateText;
    }

    public String getDistanceText() {
        return oDistanceText;
    }

    public String getAppendixText() {
        return oAppendixText;
    }

    public RecordsItem(long aID, String aDateText, String aDistanceText, String aAppendixText, Map aLocationMap){
        oId = aID;
        oDateText = aDateText;
        oDistanceText = aDistanceText;
        oAppendixText = aAppendixText;
        oLocationMap = aLocationMap;
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("id:").append(oId).append(",")
                .append("Date:").append(oDateText).append(",")
                .append("Distance:").append(oDistanceText).append(",")
                .append("AppendixText:").append(oAppendixText).append(",")
                .append("LocationMap:[");
        for(int i : oLocationMap.keySet()){
            builder.append(oLocationMap.get(i)).append(",");
        }
        builder.append("]");
        return builder.toString();

    }

}
