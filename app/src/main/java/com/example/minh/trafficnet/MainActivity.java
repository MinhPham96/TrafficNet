package com.example.minh.trafficnet;


import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void moveToMap(View view) {
        Intent intent = new Intent(this, MapView.class);
        startActivity(intent);
    }

    public void moveToHelp(View view) {
        Intent intent = new Intent(this, Help.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MainActivity","now running onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivity","now running onPause()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("MainActivity","now running onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("MainActivity","now running onRestart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("MainActivity","now running onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity","now running onDestroy()");
    }
}
