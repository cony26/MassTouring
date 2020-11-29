package com.example.masstouring.common;

import java.time.format.DateTimeFormatter;

public class Const {
    //format for LocalDateTime
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    public static final DateTimeFormatter START_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    public static final DateTimeFormatter END_SAME_DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter END_DIFF_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    public static final String NO_INFO = "no info";

    //unit
    public static final String KM_UNIT = "km";
    public static final String M_UNIT = "m";

    //parameter for location
    public static double DISTANCE_GAP = 0.5;

    //DB
    public static final String DB_NAME = "MassTouring.sqlite";
    public static final int DB_VERSION = 1;

    //Notification
    public static final String RECORD_SERVICE_NOTIFICATION_CHANNEL_ID = "RecordService";

    //RecordService Transfer
    public static final String LOCATION_UPDATE_ACTION_ID = "RecordService Action";
    public static final String LOCATION_KEY = "location";
    public static final String UPDATE_KEY = "need update";

    public static final String REPLY_CURRENT_STATE_ACTION_ID = "Reply Current State";

    //MapActivity Transfer
    public static final String START_STOP_ACTION_ID = "Start Stop Information";
    public static final String START_STOP_RECORDING_KEY = "START_STOP_RECORDING";
    public static final String START_RECORDING = "START_RECORDING";
    public static final String STOP_RECORDING = "STOP_RECORDING";
    public static final String REQUEST_CURRENT_STATE_ACTION_ID = "Request Current State";
    public static final String CURRENT_STATE = "CURRENT_STATE";
    public static final String RECORDING_ID = "RECORDING_ID";
}
