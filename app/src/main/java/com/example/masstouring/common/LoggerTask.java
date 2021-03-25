package com.example.masstouring.common;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class LoggerTask extends Thread {
    private final Context oContext;
    private final String oOutputFileName;
    private boolean oMainActivityState = false;
    private boolean oMapActivityState = false;
    private boolean oRecordServiceState = false;
    private static LoggerTask oSingleton;
    private LoggerTask(Context aContext){
        oContext = aContext;
        oOutputFileName = LocalDateTime.now().format(Const.LOG_OUTPUT_FILE_DATE_FORMAT) + ".txt";
        oMainActivityState = true;
    }

    public static void initialize(Context aContext){
        oSingleton = new LoggerTask(aContext);
        oSingleton.start();
    }

    public static LoggerTask getInstance(){
        return oSingleton;
    }

    public void setMainActivityState(boolean aMainActivityState){
        oMainActivityState = aMainActivityState;
    }

    public void setMapActivityState(boolean aMapActivityState){
        oMapActivityState = aMapActivityState;
    }

    public void setRecordServiceState(boolean aRecordServiceState){
        oRecordServiceState = aRecordServiceState;
    }

    private boolean isApplicationAlive(){
        return oMainActivityState || oMapActivityState || oRecordServiceState;
    }

    @Override
    public void run() {
        Log.e(LoggerTag.SYSTEM_PROCESS, "start logging output");

        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(new String[]{"logcat", "-v", "time"});
        }catch(IOException e){
            Log.e(LoggerTag.SYSTEM_PROCESS, "can't execute \"logcat -v time\"", e);
        }

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))){
            while(isApplicationAlive()){
                String line = reader.readLine();
                if(line == null){
                    Thread.sleep(Const.LOGGING_INTERVAL_MILLIS);
                    continue;
                }

                if(line.length() == 0){
                    Thread.sleep(Const.LOGGING_INTERVAL_MILLIS);
                    continue;
                }
                try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oContext.openFileOutput(oOutputFileName, Context.MODE_PRIVATE | Context.MODE_APPEND), StandardCharsets.UTF_8))){
                    writer.write(line);
                    writer.newLine();
                }catch(IOException e){
                    Log.e(LoggerTag.SYSTEM_PROCESS, "error happens in logging output", e);
                }
            }
        }catch(IOException | InterruptedException e){
            Log.e(LoggerTag.SYSTEM_PROCESS, "error happens in logging output", e);
        }finally {
            proc.destroy();
        }
        Log.e(LoggerTag.SYSTEM_PROCESS, "finish logging output");
    }
}
