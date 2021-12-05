package com.example.masstouring.common;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Const {
    //format for LocalDateTime
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    public static final DateTimeFormatter LOG_OUTPUT_FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
    public static final String DUMMY_DATE_FORMAT = "1000/10/01 00:00:00";
    public static final DateTimeFormatter START_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    public static final DateTimeFormatter END_SAME_DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter END_DIFF_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    public static final String NO_INFO = "no info";
    public static final ZoneOffset STORED_OFFSET = ZoneOffset.ofHours(9);

    //unit
    public static final String KM_UNIT = "km";
    public static final String M_UNIT = "m";

    //parameter for location
    public static int UPDATE_INTERVAL = 2000;
    public static int UPDATE_FASTEST_INTERVAL = 1000;
    public static double DISTANCE_GAP = 0.5;

    //DB
    public static final String DB_NAME = "MassTouring.sqlite";
    public static final int DB_VERSION = 2;
    public static final int INVALID_ID = -1;

    //Notification
    public static final String RECORD_SERVICE_NOTIFICATION_CHANNEL_ID = "RecordService";

    //Animation
    public static final int FPS_MILLIS = 1000 / 30;
    public static final int MOVING_RATE_PIXEL_PER_FPS = 5;

    //parameter for loggin
    public static final int LOGGING_INTERVAL_MILLIS = 1000;
}
