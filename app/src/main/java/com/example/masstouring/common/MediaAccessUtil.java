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
    public static List<Picture> loadPictures(Context aContext, RecordItem aRecordItem, long startDate, long endDate){
        Log.d(LoggerTag.MEDIA_ACCESS, String.format("loadPictures[startDate:endDate]=[%d,%d]", startDate, endDate));

        List<Picture> pictureList = new ArrayList<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
        };
        String selection = MediaStore.Images.Media.DATE_ADDED + " >= " + startDate + " AND " + MediaStore.Images.Media.DATE_ADDED + " <= " + endDate;
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

            while(cursor.moveToNext()){
                long id = cursor.getLong(idColumn);
                int date = cursor.getInt(dateAddedColumn);

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                LatLng nearestLatLng = fetchNearestLatLng(aRecordItem.getTimeStampMap(), aRecordItem.getLocationMap(), date);
                pictureList.add(new Picture(contentUri, date, nearestLatLng));
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
