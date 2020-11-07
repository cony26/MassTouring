package com.example.masstouring;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class RecordsItemTest {

    @Test
    public void calculateDistanceReturnZero(){
        RecordsItem oRecordsItem;
        Map<Integer, LatLng> oLocationMap;
        oLocationMap = new HashMap<>();
        oLocationMap.put(0, new LatLng(0,0));
        oLocationMap.put(1, new LatLng(0,0));
        oRecordsItem = new RecordsItem(0, "", "", oLocationMap);

        assertThat(oRecordsItem.getDistance(), is((double)0));
    }

    @Test
    public void calculateDistanceReturnNonZero(){
        RecordsItem oRecordsItem;
        Map<Integer, LatLng> oLocationMap;
        oLocationMap = new HashMap<>();
        oLocationMap.put(0, new LatLng(0,0));
        oLocationMap.put(1, new LatLng(1,0));
        oRecordsItem = new RecordsItem(0, "", "", oLocationMap);

        assertThat(oRecordsItem.getDistance(), closeTo(110574, 1));
    }
}