package com.example.minh.trafficnet;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class Help extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Help","now running onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("Help","now running onPause()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("Help","now running onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("Help","now running onRestart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("Help","now running onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("Help","now running onDestroy()");
    }
}
