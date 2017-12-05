package com.example.minh.trafficnet;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.xml.sax.helpers.XMLReaderAdapter;

import java.util.ArrayList;


public class MapView extends FragmentActivity implements
        SensorEventListener,                                    //listen to sensor event
        OnMapReadyCallback,                                     //for map set up
        GoogleApiClient.ConnectionCallbacks,                    //to connect to Google API Client for Google Map
        GoogleApiClient.OnConnectionFailedListener,             //checker for Google API Client
        com.google.android.gms.location.LocationListener {      //for current location retrieval

    private GoogleMap mMap;                         //an instance for Google Map
    private Marker myMarker;                        //an instance to create marker on Google Map
    private GoogleApiClient mGoogleApiClient;       //an instance for Google API Client
    private LocationRequest mLocationRequest;       //an instance for Location setup

    //set up the name for the map
    public static final String TAG = MapView.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private FirebaseDatabase mFirebaseDatabase;         //an instance for Firebase Database
    private DatabaseReference mDatabaseReference;       //an instance for the database listener
    private ChildEventListener mChildEventListener;     //an instance for the child listener in the database

    public static final int RC_SIGN_IN = 1;                     //a constance for sign in
    private String mUsername;                                   //an instance that holds the username
    public static final String ANONYMOUS = "anonymous";         //default username
    private FirebaseAuth mFirebaseAuth;                         //an instance for the authentication
    //an instance for the authentiation state listener
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private SharedPreferences sharedPref;       //an instance for the shared preference
    private SensorManager mSensorManager;       //an instance for the sensor manager
    private Sensor mRotate;                     //an instance to hold the sensor
    boolean lock = true;                        //the lock for the rotation feature

    StreetPoint street1;                        //an instance of a street using StreetPoint class
    //an array of StreetPoint to hold all the re-defined streets on the database
    ArrayList<StreetPoint> streets = new ArrayList<StreetPoint>();

    @Override
    public void onBackPressed() {
        //prevent user from pressing back
        //so user can only get back by signing out
        moveTaskToBack(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);
        Log.i("MapView", "onCreate is running");

        //set up the shared preference, this is private which is used within the app only
        Log.i("MapView", "create shared preference");
        sharedPref = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);

        Log.i("MapView", "setup map");
        setUpMapIfNeeded();

        Log.i("MapView", "setup Google API Client");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)           //add the connection call backs
                .addOnConnectionFailedListener(this)    //add the connection failed notice
                .addApi(LocationServices.API)           //add location services
                .build();                               //build the new Google API Client

        Log.i("MapView", "setup Location Request");
        mLocationRequest = LocationRequest.create()
                //set the priority to high accuracy to have more precise result
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                //set the interval to update the location
                .setInterval(1000);            //10 seconds, in millisecond

        Log.i("MapView", "setup Firebase Database");
        //get instance for both the database and authentiaction
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        //set the reference to specific on the "streets" child in the database
        mDatabaseReference = mFirebaseDatabase.getReference().child("streets");

        Log.i("MapView", "setup Sensor");
        //create a new sensor service for the sensor manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //set the type of sensor to gravity
        mRotate = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        Log.i("MapView", "setup Firebase Authentication");
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            //when the authentication state changed (eg. sign in, sign out)
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //get the user from the database
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //if user is signed in
                if (user != null) {
                    //Initialize the database
                    onSignedInInitialize(user.getDisplayName());
                    Log.i("MapView", "Signed In");
                    Toast.makeText(MapView.this, "Signed in", Toast.LENGTH_SHORT).show();
                }
                else {
                    Log.i("MapView", "Signed Out");
                    //stop the database acitivity
                    onSignedOutCleanUp();
                    //create a sign in menu
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()            //create a sign in instance
                                    .setIsSmartLockEnabled(false)           //disable smart lock feature
                                    .setProviders(AuthUI.EMAIL_PROVIDER)    //set the sign in type to email
                                    .build(), RC_SIGN_IN);                  //build the sign in menu
                }
            }
        };

        Log.i("MapView", "setup Search button");
        final Button buttonSearch = findViewById(R.id.buttonSearch);
        //search the street if the button is clicked
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchStreet();
            }
        });

        Log.i("MapView", "setup Lock button");
        final Button buttonLock = findViewById(R.id.buttonLock);
        buttonLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lock = !lock;       //toggle the lock
                Log.i("MapView", "Lock Status: " + String.valueOf(lock));
                if(lock) {
                    buttonLock.setText("Unlock Rotation");
                    //if the map is ready to use
                    if (mMap != null) {
                        Log.i("MapView", "Lock the rotation");
                        //get the current center location on the map
                        LatLng latlng = mMap.getCameraPosition().target;
                        //set the camera to that location and set the tilt back to 0
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                                new CameraPosition.Builder().target(latlng).tilt(0).zoom(14).build()));
                    }
                }
                else {
                    Log.i("MapView", "Unlock the rotation");
                    buttonLock.setText("Lock Rotation");
                }
            }
        });

        Log.i("MapView", "setup Sign Out Button");
        final Button buttonSignOut = findViewById(R.id.buttonSignOut);
        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sign out off the activity
                AuthUI.getInstance().signOut(MapView.this)
                    //when the sign out is completed
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            //go back to the main menu
                            Log.i("MapView", "Signed out, back to Main Menu");
                            startActivity(new Intent(MapView.this, MainActivity.class));
                            finish();       //finish this activity
                        }
                    });
            }
        });
        Log.i("MapView", "onCreate finished");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("MapView", "Check authentication");
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN) {
            if(resultCode == RESULT_OK) {
                Log.i("MapView", "Signed in");
                Toast.makeText(MapView.this, "Signed in", Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.i("MapView", "Sign in canceled");
                Toast.makeText(MapView.this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MapView", "onResume is running");
        //use the sensor manager to create a listner for the sensor
        mSensorManager.registerListener(this, mRotate, SensorManager.SENSOR_DELAY_NORMAL);
        //set up the map if needed
        setUpMapIfNeeded();
        //connect to Google API Client
        mGoogleApiClient.connect();
        //add new authentication state listener if the current is null
        if(mAuthStateListener != null) {
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
        Log.i("MapView", "onResume finished");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MapView", "onPause is running");
        //cancel the sensor and remove the authentication
        mSensorManager.unregisterListener(this);
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        //disconnect the Google API Client
        if(mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        Log.i("MapView", "onPause finished");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!lock) {     //if rotation feature is unlocked
            //get the gravity reading of Y and Z axis
            float yVal = event.values[1];
            float zVal = event.values[2];
            //calculate the angle on X axis
            float xAng = (float) ((Math.atan2(yVal, zVal) + Math.PI) * 57.3);

            if (mMap != null) {
                //get the current center on the map
                LatLng latlng = mMap.getCameraPosition().target;
                //tilt the map based on the rotation of the X axis
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder().target(latlng).tilt(Math.abs(xAng - 180)).zoom(14).build()));
                Log.i("Info", "Tilt angle : " + String.valueOf(mMap.getCameraPosition().tilt));
            }
        }
    }

    //required method for the sensor
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i("MapView", "Map is ready");
        //get the Google Map
        mMap = googleMap;
        //set to activate a method when click on the title of the marker
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker arg0) {
                //scan if the title match the street name in the array list
                for(int i = 0; i < streets.size(); i++) {
                    if(arg0 != null && arg0.getTitle().equals(streets.get(i).getName())) {
                        //send the index of the street to the shared preference
                        sharedPref.edit().putInt("Selected Street", i).apply();
                        //send the current username to the shared preference
                        sharedPref.edit().putString("Current User", mUsername).apply();
                        Log.i("Info Size", "Mode to " + streets.get(i).getName() + " Info");
                        //Move to the info page of the selected street
                        Intent intent = new Intent(MapView.this, InfoView.class);
                        startActivity(intent);
                    }
                }
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected.");

        //perform permission check to ensure that user allow location
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1600);
        }
        //get current location
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //if no location retrieved, request for location update
        if(location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            //setup current location on map
            Log.i("MapView", "Setup current location");
            handleNewLocation(location);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if(connectionResult.hasResolution()) {
            try {
                //try to run the resolution if the connection to Location services is failed
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            }
            catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.i(TAG, "Location Services connection failed with code " + connectionResult.getErrorMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            Log.i("MapView", "Map is null, requesting map");
            // Try to obtain the map from the SupportFragmentManager.
            SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFrag.getMapAsync(this);
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                Log.i("MapView", "Setup Map");
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        //set up map by create a new marker
        myMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("marker"));
    }

    private void handleNewLocation(Location location) {
        //create a custom icon to differentiate the user location with the other street point marker
        //get the resource image
        BitmapDrawable bitmapdraw =(BitmapDrawable)getResources().getDrawable(R.drawable.user_location);
        Bitmap mBitmap = bitmapdraw.getBitmap();        //add the image to the bitmap
        //create a custom bitmap with smaller size to fit the map using the above bitmap
        Bitmap smallMarker = Bitmap.createScaledBitmap(mBitmap, 130, 130, false);

        //get current location latitude and longitude
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        Log.i("MapView", "Current Latitude: " + String.valueOf(currentLatitude));
        Log.i("MapView", "Current Longitude: " + String.valueOf(currentLongitude));

        //create new latlng instance based on latitude and longitude
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        myMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)           //specify marker location
                .title("Your Location")     //marker title
                //add the custom icon to the marker
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
        //move current camera to marker position, with zoom value of 14
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));
    }

    //create a function to listen to the user input
    private void attachDatabaseReadListener() {
        Log.i("MapView", "create Database Listener for current child");
        if(mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //get the data from the database and add them to the streets array list
                    street1 = dataSnapshot.getValue(StreetPoint.class);
                    streets.add(street1);
                    //add the street on the map using the marker
                    mMap.addMarker(new MarkerOptions()
                            //the position is based on the pre-defined latitude and longitude of the street
                            .position(new LatLng(street1.getLatitude(), street1.getLongitude()))
                            //the title of the marker is the street name
                            .title(street1.getName()));
                    Log.i("MapView", street1.getName() + "  Street added");
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
            //set this child event listener to the database reference
            mDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        Log.i("MapView", "detach database read listener");
        if(mChildEventListener != null) {
            //remove the data read listener
            mDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    private void searchStreet() {
        boolean streetFound = false;
        EditText searchBar = (EditText) findViewById(R.id.search);
        //get the street name from the search bar and convert them into lowercase
        String searchStreet = searchBar.getText().toString().toLowerCase();
        searchBar.setText("");      //empty the search bar
        Log.i("MapView", "Searching street");
        for(int i = 0; i < streets.size(); i++) {
            //check if the search street match any street name in the array list
            if(searchStreet.equals(streets.get(i).getName().toLowerCase())) {
                //get the position of the found street
                LatLng latLng = new LatLng(streets.get(i).getLatitude(), streets.get(i).getLongitude());
                //set the camera to the found street
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));

                //mark that the street is found
                Log.i("MapView", "Found " + searchStreet);
                Toast.makeText(this, "Found " + searchStreet, Toast.LENGTH_LONG).show();
                streetFound = true;
                break;
            }
        }
        //notify user if the street is not found
        if(!streetFound) {
            Log.i("MapView", "Cannot found " + searchStreet);
            Toast.makeText(this, "No street found", Toast.LENGTH_LONG).show();
        }
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        //attach the database read listener after signed in
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanUp() {
        mUsername = ANONYMOUS;          //set username back to default
        streets.clear();                //clear the array list
        detachDatabaseReadListener();   //detach the listener
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("MapView","now running onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("MapView","now running onRestart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("MapView","now running onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MapView","now running onDestroy()");
    }
}
