package com.example.minh.trafficnet;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;

public class InfoView extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;         //an instance for Firebase Database
    private DatabaseReference mDatabaseReference;       //an instance for the database listener
    private ChildEventListener mChildEventListener;     //an instance for the child listener in the database

    private SharedPreferences sharedPref;               //an instance for the shared preference
    //an array of StreetPoint to hold all the re-defined streets on the database
    ArrayList<StreetPoint> streets = new ArrayList<StreetPoint>();
    //a street point variable to hold the current street point
    StreetPoint current_street;
    int index;              //an index passed by the shared preference to locate the selected street in the array list
    String username;        //the current username passed by the shared preference to indicate who editing the street

    //the flag for the radio buttons that indicate the available status of the street for update
    //when one radio button is selected, the other will be off, so one of them is set to true
    private boolean rbflag1 = true;
    private boolean rbflag2 = false;
    private boolean rbflag3 = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("Info View","onCreate is running");
        setContentView(R.layout.activity_info_view);

        Log.i("Info View","Get the shared preference values");
        //the mode of the shared preference is set to private to use within this app only
        sharedPref = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);
        //get the index and username from the previous activity (MapView)
        index = sharedPref.getInt("Selected Street",0);
        username = sharedPref.getString("Current User", "anoymous");

        Log.i("Info View","Setup Firebase database");
        //initialize the database and its reference
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabaseReference = mFirebaseDatabase.getReference().child("streets");

        //attach the listener
        attachDatabaseReadListener();

        Log.i("Info View","Setup radio buttons");
        final RadioButton rb1 = (RadioButton) findViewById(R.id.radioButton);
        final RadioButton rb2 = (RadioButton) findViewById(R.id.radioButton2);
        final RadioButton rb3 = (RadioButton) findViewById(R.id.radioButton3);
        rb1.setChecked(true);

        rb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rb1.isChecked()) {
                    if (!rbflag1) {
                        rb1.setChecked(true);
                        rbflag1 = true;
                        Log.i("Info View","Turn on radio button 1");
                        rb2.setChecked(false);
                        rbflag2 = false;
                        rb3.setChecked(false);
                        rbflag3 = false;
                    }}}});

        rb2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rb2.isChecked()) {
                    if (!rbflag2) {
                        rb2.setChecked(true);
                        rbflag2 = true;
                        Log.i("Info View","Turn on radio button 2");
                        rb1.setChecked(false);
                        rbflag1 = false;
                        rb3.setChecked(false);
                        rbflag3 = false;
                    }}}});

        rb3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rb3.isChecked()) {
                    if (!rbflag3) {
                        rb3.setChecked(true);
                        rbflag3 = true;
                        Log.i("Info View","Turn on radio button 3");
                        rb1.setChecked(false);
                        rbflag1 = false;
                        rb2.setChecked(false);
                        rbflag2 = false;
                    }}}});

    }

    //create a function to listen to the user input
    private void attachDatabaseReadListener() {
        if(mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //get the data from the database and add them to the streets array list
                    StreetPoint street1 = dataSnapshot.getValue(StreetPoint.class);
                    streets.add(street1);
                    //since this method takes time to get all the data from the database
                    //the info display of the street will be performed when the array list size is greater than the index
                    //this indicates that the info of the selected street is retrieved to the array list
                    //the info display is done in this method instead of the onCreate since the onCreate is executed first
                    //which will cause error because the array list does not have any value initially and have to wait for the database to response
                    if(streets.size() > index) {
                        //get the current street point from the array list
                        current_street = streets.get(index);
                        Log.i("Info View","Street info added");
                        //since the name and status are pre-defined in the database
                        //both of them can be retrieved and display on app
                        TextView name = (TextView) findViewById(R.id.nameText);
                        name.setText(current_street.getName());
                        TextView status = (TextView) findViewById(R.id.statusText);
                        status.setText(current_street.printStatus());
                        //while both the time modified and edited by are not pre-defined
                        //so the program need to check if they are not null to display
                        //since both of them go together so the program just need to check one
                        if(current_street.getTimeModified() != null) {
                            TextView time = (TextView) findViewById(R.id.timeText);
                            time.setText(current_street.printTimeModified());
                            TextView editedBy = (TextView) findViewById(R.id.editedByText);
                            editedBy.setText(current_street.printEditedBy());
                        }
                    }
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }


    //this method is executed when the user click the save button
    //this is called in the onClick in the activity layout of the save button
    public void saveStatus(View view) {
        Log.i("Info View","Saving new status");
        //check which status is selected
        int newStatus = 0;
        if (rbflag2) newStatus = 1;
        else if (rbflag3) newStatus = 2;
        current_street.setStatus(newStatus);            //set the new status
        current_street.setTimeModified(new Date());     //set the new modified date
        current_street.setEditedBy(username);           //set the new edited by username
        //each street is stored in the database with a child name based on the street name with no space
        //so to get the child, the reference needs to get the street name and remove the space
        //then update that street based on the new StreetPoint instance
        mDatabaseReference.child(current_street.getName().replaceAll("\\s",""))
                .setValue(current_street);
        Log.i("Info View","Update done");
        Log.i("Info View","Move back to MapView");
        //when this is done, move back to the MapView activity
        Intent intent = new Intent(InfoView.this, MapView.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("InfoView","now running onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("InfoView","now running onPause()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("InfoView","now running onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("InfoView","now running onRestart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("InfoView","now running onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("InfoView","now running onDestroy()");
    }
}
