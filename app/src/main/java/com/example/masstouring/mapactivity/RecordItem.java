package com.example.masstouring.mapactivity;

import android.location.Location;

import com.example.masstouring.common.Const;
import com.google.android.gms.maps.model.LatLng;

import java.time.LocalDateTime;
import java.util.Map;

public class RecordItem {
    private final int oId;
    private final LocalDateTime oStartDate;
    private final LocalDateTime oEndDate;
    private final double oDistance;
    private final Map<Integer, LatLng> oLocationMap;
    private final Map<Integer, String> oTimeStampMap;
    private final Map<Integer, Double> oSpeedkmphMap;
    private boolean oSelected = false;
    public static final RecordItem EMPTY_RECORD = new RecordItem();

    public RecordItem(int aID, String aStartDateText, String aEndDateText, Map aLocationMap, Map aTimeStampMap, Map aSpeedKmphMap){
        oId = aID;

        oStartDate = LocalDateTime.parse(aStartDateText, Const.DATE_FORMAT);

        if(aEndDateText.equals(Const.NO_INFO)){
            oEndDate = null;
        }else {
            oEndDate = LocalDateTime.parse(aEndDateText, Const.DATE_FORMAT);
        }

        oLocationMap = aLocationMap;
        oTimeStampMap = aTimeStampMap;
        oSpeedkmphMap = aSpeedKmphMap;
        oDistance = calculateDistance(aLocationMap);
    }

    private RecordItem(){
        oId = 0;
        oStartDate = LocalDateTime.parse(Const.DUMMY_DATE_FORMAT, Const.DATE_FORMAT);;
        oEndDate = LocalDateTime.parse(Const.DUMMY_DATE_FORMAT, Const.DATE_FORMAT);
        oLocationMap = null;
        oTimeStampMap = null;
        oSpeedkmphMap = null;
        oDistance = 0;
    }

    public LocalDateTime getStartDate(){
        return oStartDate;
    }

    public LocalDateTime getEndDate(){
        return oEndDate;
    }

    public int getId() {
        return oId;
    }

    public double getDistance() {
        return oDistance;
    }

    public Map<Integer, LatLng> getLocationMap() {
        return oLocationMap;
    }

    public Map<Integer, String> getTimeStampMap(){
        return oTimeStampMap;
    }

    public Map<Integer, Double> getSpeedkmphMap(){ return oSpeedkmphMap;}

    public String getYearText() {
        return Integer.toString(oStartDate.getYear());
    }

    public boolean isSelected() {
        return oSelected;
    }

    public void setSelected(boolean aIsSelected) {
         oSelected = aIsSelected;
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("id:").append(oId).append(",")
                .append("Year:").append(getYearText()).append(",")
                .append("StartDate:").append(oStartDate).append(",")
                .append("EndDate:").append(oEndDate).append(",")
                .append("Distance:").append(oDistance).append(",")
                .append("Locations:[date, <latitude, longitude>]=");
        for(int i = 0; i < oLocationMap.size(); i++){
            builder.append("[")
                    .append(oTimeStampMap.get(i)).append(",<")
                    .append(oLocationMap.get(i).latitude).append(",")
                    .append(oLocationMap.get(i).longitude).append(">],");
        }
        return builder.toString();
    }

    private float calculateDistance(Map<Integer, LatLng> aLocationMap){
        float distanceSum = 0;
        float[] distancePoints = new float[1];
        for(int i = 1; i < aLocationMap.size(); i++){
            LatLng latLngFrom = aLocationMap.get(i - 1);
            LatLng latLngTo = aLocationMap.get(i);

            Location.distanceBetween(
                    latLngFrom.latitude, latLngFrom.longitude,
                    latLngTo.latitude, latLngTo.longitude,
                    distancePoints
            );
            distanceSum += distancePoints[0];
        }
        return distanceSum;
    }
}
