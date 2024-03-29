package com.example.masstouring.common;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.example.masstouring.mapactivity.Picture;
import com.example.masstouring.mapactivity.RecordItem;
import com.google.android.gms.maps.model.LatLng;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MediaAccessUtil {
    /**
     * load pictures from Media Storage between {@link RecordItem#getStartDate()} and {@link RecordItem#getEndDate()}.<br>
     * The position, {@link LatLng}, of {@link Picture} is fetched with the nearest recorded point in time.
     * @param aContext
     * @param aRecordItem
     * @return list of {@link Picture}
     */
    public static List<Picture> loadPictures(Context aContext, RecordItem aRecordItem){
        long startDateSecond = aRecordItem.getStartDate().toEpochSecond(Const.STORED_OFFSET);

        long endDateSecond;
        Map<Integer, String> timeStampMap = aRecordItem.getTimeStampMap();
        if(aRecordItem.getEndDate() == null){
            endDateSecond = LocalDateTime.parse(timeStampMap.get(timeStampMap.size() - 1), Const.DATE_FORMAT).toEpochSecond(Const.STORED_OFFSET);
        } else {
            endDateSecond = aRecordItem.getEndDate().toEpochSecond(Const.STORED_OFFSET);
        }

        Log.d(LoggerTag.MEDIA_ACCESS, String.format("loadPictures[startDate:endDate]=[%d,%d]", startDateSecond, endDateSecond));

        List<Picture> pictureList = new ArrayList<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.ORIENTATION
        };
        String selection = MediaStore.Images.Media.DATE_ADDED + " >= " + startDateSecond + " AND " + MediaStore.Images.Media.DATE_ADDED + " <= " + endDateSecond;
        String[] selectionArgs = new String[]{};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " ASC";

        try(Cursor cursor = aContext.getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )){
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
            int orientationColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION);

            while(cursor.moveToNext()){
                long id = cursor.getLong(idColumn);
                int date = cursor.getInt(dateAddedColumn);
                int orientation = cursor.getInt(orientationColumn);

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                LatLng nearestLatLng = fetchNearestLatLng(aRecordItem.getTimeStampMap(), aRecordItem.getLocationMap(), date);
                pictureList.add(new Picture(contentUri, date, nearestLatLng, orientation));
            }
        }

        for(Picture picture : pictureList){
            Log.d(LoggerTag.MEDIA_ACCESS, picture.toString());
        }

        return pictureList;
    }

    private static LatLng fetchNearestLatLng(Map<Integer, String> aTimeStampMap, Map<Integer, LatLng> aLocationMap, int aFetchTimeStamp){
        long candidateTimeStamp;
        int size = aTimeStampMap.size();
        int fetchIndex = size - 1;
        for(int i = 0; i < size; i++){
            candidateTimeStamp = LocalDateTime.parse(aTimeStampMap.get(i), Const.DATE_FORMAT).toEpochSecond(Const.STORED_OFFSET);
            if(candidateTimeStamp >= aFetchTimeStamp){
                fetchIndex = i;
                break;
            }
        }

        return aLocationMap.get(fetchIndex);
    }
}
