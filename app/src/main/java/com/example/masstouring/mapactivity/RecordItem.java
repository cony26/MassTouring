package com.example.masstouring.mapactivity;

import android.graphics.Color;
import android.location.Location;
import android.util.Log;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecordItem {
    private final int oId;
    private final LocalDateTime oStartDate;
    private final LocalDateTime oEndDate;
    private final double oDistance;
    private final Map<Integer, LatLng> oLocationMap;
    private final Map<Integer, String> oTimeStampMap;
    private final Map<Integer, Double> oSpeedkmphMap;
    private boolean oSelected = false;
    private boolean oRendered = false;
    private final boolean oHasAllData;
    private static final int alpha = 0x80000000;
    public static final int INVALID_ID = -1;
    public static final RecordItem EMPTY_RECORD = new RecordItem(INVALID_ID);

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
        oHasAllData = true;
    }

    public RecordItem(int aId){
        oId = aId;
        oStartDate = LocalDateTime.parse(Const.DUMMY_DATE_FORMAT, Const.DATE_FORMAT);;
        oEndDate = LocalDateTime.parse(Const.DUMMY_DATE_FORMAT, Const.DATE_FORMAT);
        oLocationMap = null;
        oTimeStampMap = null;
        oSpeedkmphMap = null;
        oDistance = 0;
        oHasAllData = false;
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

    public void setRendered(boolean aIsRendered){
        oRendered = aIsRendered;
    }

    public boolean isRendered(){
        return oRendered;
    }

    public boolean hasAllData(){
        return oHasAllData;
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

    public List<PolylineOptions> createPolylineOptions(){
        double maxSpeedKmph = oSpeedkmphMap.values().stream().max(Double::compareTo).orElse(0.0);
        double minSpeedKmph = oSpeedkmphMap.values().stream().min(Double::compareTo).orElse(0.0);
        double diff = maxSpeedKmph - minSpeedKmph;

        List<PolylineOptions> polylineOptionsList = new ArrayList<>();
        PolylineOptions polylineOptions = null;
        int prevColor = Integer.MIN_VALUE;

        for(int i = 0; i < oLocationMap.size(); i++){
            int color = calculateColorForNewSpeed(oSpeedkmphMap.get(i), diff, minSpeedKmph);

            if(polylineOptions == null){
                polylineOptions = new PolylineOptions().color(color);
                prevColor = color;
                continue;
            }

            if(color != prevColor){
                polylineOptionsList.add(polylineOptions);
                polylineOptions = new PolylineOptions().color(color);
            }
            polylineOptions.add(oLocationMap.get(i));
            prevColor = color;
        }
        return polylineOptionsList;
    }

    private int calculateColorForNewSpeed(double aNewSpeedKmph, double aDiffSpeedKmph, double aMinSpeedKmph){
        int color;
        if(aNewSpeedKmph < aMinSpeedKmph + aDiffSpeedKmph / 3){
            color = Color.RED + alpha;
        }else if(aNewSpeedKmph < aMinSpeedKmph + aDiffSpeedKmph * 2 / 3){
            color = Color.GREEN + alpha;
        }else{
            color = Color.BLUE + alpha;
        }

        return color;
    }

    public LatLngBounds createFitAreaFrom(){
        double minLat;
        double maxLat;
        double minLon;
        double maxLon;
        Set<Double> latSet = new HashSet<>();
        Set<Double> lonSet = new HashSet<>();

        for(int i = 0; i < oLocationMap.size(); i++){
            LatLng latLng = oLocationMap.get(i);
            latSet.add(latLng.latitude);
            lonSet.add(latLng.longitude);
        }
        maxLat = latSet.stream().max(Double::compareTo).get();
        minLat = latSet.stream().min(Double::compareTo).get();
        maxLon = lonSet.stream().max(Double::compareTo).get();
        minLon = lonSet.stream().min(Double::compareTo).get();

        LatLngBounds area = new LatLngBounds(
                new LatLng(minLat, minLon),
                new LatLng(maxLat, maxLon)
        );
        StringBuilder builder = new StringBuilder();
        builder.append("FitArea:")
                .append("[lat1, lon1] = [").append(minLat).append(",").append(minLon).append("]")
                .append("[lat2, lon2] = [").append(maxLat).append(",").append(maxLon).append("]");
        Log.d(LoggerTag.LOCATION, builder.toString());

        return area;
    }
}
