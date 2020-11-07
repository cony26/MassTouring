package com.example.masstouring;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.Map;

public class RecordsItem {
    private final int oId;
    private final String oStartDateText;
    private final String oEndDateText;
    private final double oDistance;
    private final String oAppendixText;
    private final Map<Integer, LatLng> oLocationMap;

    public RecordsItem(int aID, String aStartDateText, String aEndDateText, Map aLocationMap){
        oId = aID;
        oStartDateText = aStartDateText;
        oEndDateText = aEndDateText;
        oLocationMap = aLocationMap;
        oDistance = calculateDistance();
        oAppendixText = buildAppendixText();
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("id:").append(oId).append(",")
                .append("Date:").append(oStartDateText).append(",")
                .append("Distance:").append(oDistance).append(",")
                .append("AppendixText:").append(oAppendixText).append(",")
                .append("LocationMap:[");
        for(int i : oLocationMap.keySet()){
            builder.append(oLocationMap.get(i)).append(",");
        }
        builder.append("]");
        return builder.toString();

    }

    private float calculateDistance(){
        float distanceSum = 0;
        float[] distancePoints = new float[1];
        for(int i = 1; i < oLocationMap.size(); i++){
            LatLng latLngFrom = oLocationMap.get(i - 1);
            LatLng latLngTo = oLocationMap.get(i);

            Location.distanceBetween(
                    latLngFrom.latitude, latLngFrom.longitude,
                    latLngTo.latitude, latLngTo.longitude,
                    distancePoints
            );
            distanceSum += distancePoints[0];
        }
        return distanceSum;
    }

    private String buildAppendixText(){
        StringBuilder builder = new StringBuilder();
        builder.append(oId);
        return builder.toString();
    }

    public String getDateText(){
        StringBuilder builder = new StringBuilder();
        builder.append(oStartDateText).append("-").append(oEndDateText);
        return builder.toString();
    }

    public String getDistanceText(){
        StringBuilder builder = new StringBuilder();
        builder.append(Math.round(oDistance / 1000)).append("km");
        return builder.toString();
    }

    public String getAppendixText() {
        return oAppendixText;
    }

    public int getId() {
        return oId;
    }

    double getDistance() {
        return oDistance;
    }

    public Map<Integer, LatLng> getLocationMap() {
        return oLocationMap;
    }
}
