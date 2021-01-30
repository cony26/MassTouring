package com.example.masstouring.mapactivity;

import android.location.Location;

import com.example.masstouring.common.Const;
import com.google.android.gms.maps.model.LatLng;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class RecordItem {
    private final int oId;
    private final String oYearText;
    private final String oStartDateText;
    private final String oEndDateText;
    private final double oDistance;
    private final String oAppendixText;
    private final Map<Integer, LatLng> oLocationMap;
    private final Map<Integer, LatLng> oTimeStampMap;
    private final Map<Integer, Double> oSpeedkmphMap;
    private boolean oSelected = false;

    public RecordItem(int aID, String aStartDateText, String aEndDateText, Map aLocationMap, Map aTimeStampMap, Map aSpeedKmphMap){
        oId = aID;

        LocalDateTime startDate = LocalDateTime.parse(aStartDateText, Const.DATE_FORMAT);
        oYearText = Integer.toString(startDate.getYear());
        oStartDateText = startDate.format(Const.START_DATE_FORMAT);

        if(aEndDateText.equals(Const.NO_INFO)){
            oEndDateText = Const.NO_INFO;
        }else {
            LocalDateTime endDate = LocalDateTime.parse(aEndDateText, Const.DATE_FORMAT);
            DateTimeFormatter format;
            if (startDate.getDayOfMonth() == endDate.getDayOfMonth()) {
                format = Const.END_SAME_DATE_FORMAT;
            } else {
                format = Const.END_DIFF_DATE_FORMAT;
            }
            oEndDateText = "-" + endDate.format(format);
        }

        oLocationMap = aLocationMap;
        oTimeStampMap = aTimeStampMap;
        oSpeedkmphMap = aSpeedKmphMap;
        oDistance = calculateDistance();
        oAppendixText = buildAppendixText();
    }

    public String getYearText() {
        return oYearText;
    }

    public String getStartDateText() {
        return oStartDateText;
    }

    public String getEndDateText() {
        return oEndDateText;
    }

    public String getDistanceText(){
        StringBuilder builder = new StringBuilder();
        BigDecimal distance = new BigDecimal(oDistance / 1000);
        builder.append(distance.setScale(3, BigDecimal.ROUND_UP)).append(Const.KM_UNIT);
        return builder.toString();
    }

    public String getAppendixText() {
        return oAppendixText;
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

    public Map<Integer, Double> getSpeedkmphMap(){ return oSpeedkmphMap;}

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
                .append("Year:").append(oYearText).append(",")
                .append("StartDate:").append(oStartDateText).append(",")
                .append("EndDate:").append(oEndDateText).append(",")
                .append("Distance:").append(oDistance).append(",")
                .append("AppendixText:").append(oAppendixText).append(",")
                .append("Locations:[date, <latitude, longitude>]=");
        for(int i = 0; i < oLocationMap.size(); i++){
            builder.append("[")
                    .append(oTimeStampMap.get(i)).append(",<")
                    .append(oLocationMap.get(i).latitude).append(",")
                    .append(oLocationMap.get(i).longitude).append(">],");
        }
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
}
