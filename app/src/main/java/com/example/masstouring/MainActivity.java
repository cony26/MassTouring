package com.example.masstouring;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masstouring.common.LoggerTag;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        Log.d(LoggerTag.PROCESS,"onCreate MainActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonClickListeners();
    }

    private void setButtonClickListeners(){
        findViewById(R.id.btnOpenMap).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i = new Intent(MainActivity.this, MapActivity.class);
                        startActivity(i);
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LoggerTag.PROCESS,"onDestroy MainActivity");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LoggerTag.PROCESS,"onPause MainActivity");
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(LoggerTag.PROCESS,"onResume MainActivity");
    }
}
