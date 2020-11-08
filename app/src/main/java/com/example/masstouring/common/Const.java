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
}
