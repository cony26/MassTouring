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
    private final int id;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final double distance;
    private final Map<Integer, LatLng> locationMap;
    private final Map<Integer, String> timeStampMap;
    private final Map<Integer, Double> speedkmphMap;
    private final boolean selected;
    private final boolean rendered;
    private final boolean hasAllData;
    private static final int alpha = 0x80000000;
    public static final int INVALID_ID = -1;
    public static final RecordItem EMPTY_RECORD = new RecordItem(INVALID_ID);

    public RecordItem(int aID, String aStartDateText, String aEndDateText, Map aLocationMap, Map aTimeStampMap, Map aSpeedKmphMap){
        id = aID;

        startDate = LocalDateTime.parse(aStartDateText, Const.DATE_FORMAT);

        if(aEndDateText.equals(Const.NO_INFO)){
            endDate = null;
        }else {
            endDate = LocalDateTime.parse(aEndDateText, Const.DATE_FORMAT);
        }

        locationMap = aLocationMap;
        timeStampMap = aTimeStampMap;
        speedkmphMap = aSpeedKmphMap;
        distance = calculateDistance(aLocationMap);
        selected = false;
        rendered = false;
        hasAllData = true;
    }

    public RecordItem(int aId){
        id = aId;
        startDate = LocalDateTime.parse(Const.DUMMY_DATE_FORMAT, Const.DATE_FORMAT);;
        endDate = LocalDateTime.parse(Const.DUMMY_DATE_FORMAT, Const.DATE_FORMAT);
        locationMap = null;
        timeStampMap = null;
        speedkmphMap = null;
        distance = 0;
        selected = false;
        rendered = false;
        hasAllData = false;
    }

    private RecordItem(RecordItem aRecordItem, boolean aRendered, boolean aSelected, boolean aHasAllData){
        id = aRecordItem.id;
        startDate = aRecordItem.startDate;
        endDate = aRecordItem.endDate;
        locationMap = aRecordItem.locationMap;
        timeStampMap = aRecordItem.timeStampMap;
        speedkmphMap = aRecordItem.speedkmphMap;
        distance = aRecordItem.distance;
        selected = aSelected;
        rendered = aRendered;
        hasAllData = aHasAllData;
    }

    public static RecordItem createNewReloadRecordItem(RecordItem aRecordItem){
        return new RecordItem(aRecordItem, aRecordItem.rendered, aRecordItem.selected, false);
    }

    public static RecordItem createNewSelectedRecordItem(RecordItem aRecordItem, boolean aSelected){
        return new RecordItem(aRecordItem, aRecordItem.rendered, aSelected, aRecordItem.hasAllData);
    }

    public static RecordItem createNewRenderedRecordItem(RecordItem aRecordItem, boolean aRendered){
        return new RecordItem(aRecordItem, aRendered, aRecordItem.selected, aRecordItem.hasAllData);
    }

    public LocalDateTime getStartDate(){
        return startDate;
    }

    public LocalDateTime getEndDate(){
        return endDate;
    }

    public int getId() {
        return id;
    }

    public double getDistance() {
        return distance;
    }

    public Map<Integer, LatLng> getLocationMap() {
        return locationMap;
    }

    public Map<Integer, String> getTimeStampMap(){
        return timeStampMap;
    }

    public Map<Integer, Double> getSpeedkmphMap(){ return speedkmphMap;}

    public String getYearText() {
        return Integer.toString(startDate.getYear());
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isRendered(){
        return rendered;
    }

    public boolean hasAllData(){
        return hasAllData;
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append("id:").append(id).append(",")
                .append("Year:").append(getYearText()).append(",")
                .append("StartDate:").append(startDate).append(",")
                .append("EndDate:").append(endDate).append(",")
                .append("Distance:").append(distance).append(",")
                .append("Locations:[date, <latitude, longitude>]=");
        for(int i = 0; i < locationMap.size(); i++){
            builder.append("[")
                    .append(timeStampMap.get(i)).append(",<")
                    .append(locationMap.get(i).latitude).append(",")
                    .append(locationMap.get(i).longitude).append(">],");
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

        double maxSpeedKmph = speedkmphMap.values().stream().max(Double::compareTo).orElse(0.0);
        double minSpeedKmph = speedkmphMap.values().stream().min(Double::compareTo).orElse(0.0);
        double diff = maxSpeedKmph - minSpeedKmph;

        List<PolylineOptions> polylineOptionsList = new ArrayList<>();
        PolylineOptions polylineOptions = null;
        int prevColor = Integer.MIN_VALUE;

        for(int i = 0; i < locationMap.size(); i++){
            int color = calculateColorForNewSpeed(speedkmphMap.get(i), diff, minSpeedKmph);

            if(polylineOptions == null){
                polylineOptions = new PolylineOptions().color(color);
                prevColor = color;
                continue;
            }

            if(color != prevColor){
                polylineOptionsList.add(polylineOptions);
                polylineOptions = new PolylineOptions().color(color);
            }
            polylineOptions.add(locationMap.get(i));
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

        for(int i = 0; i < locationMap.size(); i++){
            LatLng latLng = locationMap.get(i);
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
