package com.example.masstouring.common;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LoggerTask extends Thread {
    private final Context oContext;
    private final String oOutputFileName;
    private boolean oMainActivityState = false;
    private boolean oMapActivityState = false;
    private boolean oRecordServiceState = false;
    private static LoggerTask oSingleton = null;
    private LoggerTask(Context aContext){
        oContext = aContext;
        oOutputFileName = LocalDateTime.now().format(Const.LOG_OUTPUT_FILE_DATE_FORMAT);
        oMainActivityState = true;
    }

    public static void initialize(Context aContext){
        if(oSingleton == null){
            oSingleton = new LoggerTask(aContext);
            oSingleton.start();
        }
    }

    public static LoggerTask getInstance(){
        return oSingleton;
    }

    boolean isLoggingCompleted(){
        return oLoggingCompleted;
    }

    @Override
    public void run() {
        while(!oApplicationLifeCycleObserver.isAnyProcessRunning()){
            try{
                Thread.sleep(100);
            }catch(InterruptedException e){
                //do nothing
            }
        }
        Log.i(LoggerTag.LOG_PROCESS, "start logging output");

        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec(new String[]{"logcat", "-v", "time"});
        }catch(IOException e){
            Log.e(LoggerTag.LOG_PROCESS, "can't execute \"logcat -v time\"", e);
        }

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))){
            while(oApplicationLifeCycleObserver.isAnyProcessRunning()){
                String line = reader.readLine();
                if(line == null || line.length() == 0){
                    Thread.sleep(Const.LOGGING_INTERVAL_MILLIS);
                    continue;
                }

                try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oContext.openFileOutput(oOutputFileName + ".txt", Context.MODE_PRIVATE | Context.MODE_APPEND), StandardCharsets.UTF_8))){
                    writer.write(line);
                    writer.newLine();
                }catch(IOException e){
                    Log.e(LoggerTag.LOG_PROCESS, "error happens in logging output", e);
                }
            }
        }catch(IOException | InterruptedException e){
            Log.e(LoggerTag.SYSTEM_PROCESS, "error happens in logging output", e);
            oOutputFileName += "_exception";
        }finally {
            proc.destroy();
            Log.i(LoggerTag.SYSTEM_PROCESS, "finish logging output");
        }

        zipFile(oOutputFileName + ".zip", oOutputFileName + ".txt");

        oContext.deleteFile(oOutputFileName + ".txt");
        Log.i(LoggerTag.SYSTEM_PROCESS, "successfully deleted the original log file.");
    }

    private boolean zipFile(String aZipFileName, String aOriginalFileName){
        byte[] buf = new byte[1024];

        try(ZipOutputStream zos = new ZipOutputStream(oContext.openFileOutput(aZipFileName, Context.MODE_PRIVATE | Context.MODE_APPEND), StandardCharsets.UTF_8);
            FileInputStream is = oContext.openFileInput(aOriginalFileName);
        ){
            ZipEntry ze = new ZipEntry(aOriginalFileName);
            zos.putNextEntry(ze);
            int len = 0;
            while((len = is.read(buf)) != -1){
                zos.write(buf, 0, len);
            }

        }catch(IOException e){
            Log.e(LoggerTag.LOG_PROCESS, "error happens in zipping output file", e);
            return false;
        }

        Log.i(LoggerTag.LOG_PROCESS, "successfully zipped:" + aZipFileName);
        return true;
    }
}
