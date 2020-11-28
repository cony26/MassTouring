package com.example.masstouring;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class RecordsItemTest {

    @Test
    public void calculateDistanceReturnZero(){
        RecordItem oRecordsItem;
        Map<Integer, LatLng> oLocationMap = new HashMap<>();
        oLocationMap.put(0, new LatLng(0,0));
        oLocationMap.put(1, new LatLng(0,0));

        Map<Integer, String> oTimeStampMap = new HashMap<>();
        oRecordsItem = new RecordItem(0, "", "", oLocationMap, oTimeStampMap);

        assertThat(oRecordsItem.getDistance(), is((double)0));
    }

    @Test
    public void calculateDistanceReturnNonZero(){
        RecordItem oRecordsItem;
        Map<Integer, LatLng> oLocationMap;
        oLocationMap = new HashMap<>();
        oLocationMap.put(0, new LatLng(0,0));
        oLocationMap.put(1, new LatLng(1,0));
        oRecordsItem = new RecordItem(0, "", "", oLocationMap, null);

        assertThat(oRecordsItem.getDistance(), closeTo(110574, 1));
    }
}