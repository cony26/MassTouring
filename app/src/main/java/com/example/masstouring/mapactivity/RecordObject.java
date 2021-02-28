package com.example.masstouring.mapactivity;

import android.location.Location;
import android.util.Log;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.google.android.gms.maps.model.LatLng;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The entity of Recording Object that is used only during recording.
 */
public class RecordObject {
    private final int oRecordId;
    private int oRecordNumber;
    private String oStartDate;
    private String oEndDate;
    private final List<Location> oLocations= new ArrayList<>();

    public RecordObject(DatabaseHelper aDatabaseHelper){
        oRecordId = aDatabaseHelper.getUniqueID();
        oRecordNumber = -1;
        oStartDate = LocalDateTime.now().format(Const.DATE_FORMAT);
    }

    private RecordObject(int aId){
        oRecordId = aId;
    }

    public static RecordObject createRecordObjectForRestore(int aId){
        return new RecordObject(aId);
    }

    public int getRecordId() {
        return oRecordId;
    }

    public int getRecordNumber() {
        return oRecordNumber;
    }

    public void setRecordNumber(int aRecordNumber){
        oRecordNumber = aRecordNumber;
    }

    public void inclementRecordNumber() {
        oRecordNumber++;
    }

    public String getStartDate() {
        return oStartDate;
    }

    public void setStartDate(String aStartDate){
        oStartDate = aStartDate;
    }

    public String getEndDate() {
        return oEndDate;
    }

    public void setEndDate(String aEndDate) {
        oEndDate = aEndDate;
    }

    public void addLocation(Location aLocation){
        oLocations.add(aLocation);
    }

    public boolean hasRecords(){
        return oRecordNumber >= 0;
    }

    public boolean isDifferenceEnough(Location aLocation) {
        if (!hasRecords()) {
            return true;
        }

        float distance = aLocation.distanceTo(getLastLocation());

        Log.d(LoggerTag.LOCATION, "(latitude, longitude) = (" + aLocation.getLatitude() + "," + aLocation.getLongitude() + ")");
        Log.d(LoggerTag.LOCATION, "(distance, limit) = (" + distance + "," + Const.DISTANCE_GAP + ")");

        if (distance >= Const.DISTANCE_GAP) {
            return true;
        } else {
            return false;
        }
    }

    public Location getLastLocation(){
        assert !hasRecords() : "no last location";

        return oLocations.get(oRecordNumber);
    }
}
