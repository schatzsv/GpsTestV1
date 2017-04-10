package com.schatzsv.gpstestv1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

import java.text.DateFormat;
import java.util.LinkedList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    protected static final String TAG = "MainActivity";

    protected String mLatitudeLabel = "Lat";
    protected String mLongitudeLabel = "Lon";

    protected TextView mLatitudeText;
    protected TextView mLongitudeText;
    protected TextView mCountText;
    protected TextView mTimeText;
    protected TextView mFeetText;
    protected TextView mCumMilesText;
    protected TextView mAccValidText;
    protected TextView mAccuracyText;
    protected TextView mMotionCountText;
    protected TextView mMotionTimeText;

    protected double mCumMiles = 0;

    /*
    ** Android UI related overrides and methods
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLatitudeText = (TextView) findViewById(R.id.latitude_text);
        mLongitudeText = (TextView) findViewById(R.id.longitude_text);
        mCountText = (TextView) findViewById(R.id.count_text);
        mTimeText = (TextView) findViewById(R.id.time_text);
        mFeetText = (TextView) findViewById(R.id.feet_text);
        mCumMilesText = (TextView) findViewById(R.id.cum_miles_text);
        mAccValidText = (TextView) findViewById(R.id.acc_valid_text);
        mAccuracyText = (TextView) findViewById(R.id.accuracy_text);
        mMotionCountText = (TextView) findViewById(R.id.motion_count_text);
        mMotionTimeText = (TextView) findViewById(R.id.motion_time_text);

        // Create an instance of GoogleAPIClient.
        doInitGoogleLocationApi();

        // Check location services permission
        doCheckLocationPermission();

        // Motion Detect Sensor
        mSigMotionSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSigMotionSensor = mSigMotionSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        if (mSigMotionSensor == null) {
            Toast.makeText(this, "Significant Motion Sensor not available!", Toast.LENGTH_LONG).show();
        }
        mSigMotionTriggerEventListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                mMotionCount++;
                mMotionLastTime = mMotionCurrentTime;
                mMotionCurrentTime = event.timestamp;
                mSigMotionSensorManager.requestTriggerSensor(mSigMotionTriggerEventListener, mSigMotionSensor);
            }
        };
        mSigMotionSensorManager.requestTriggerSensor(mSigMotionTriggerEventListener, mSigMotionSensor);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart()");
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
    }

    protected void stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates()");
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    private void updateUI() {
        int s = mLocations.size();
        if (s > 0) {
            Location l = (Location) mLocations.getLast();
            mLatitudeText.setText(String.valueOf(l.getLatitude()));
            mLongitudeText.setText(String.valueOf(l.getLongitude()));
            mTimeText.setText(DateFormat.getTimeInstance().format(l.getTime()));
            mCountText.setText(String.valueOf(s));
            if (l.hasAccuracy()) {
                mAccValidText.setText("VALID");
            } else {
                mAccValidText.setText("invalid");
            }
            mAccuracyText.setText(String.valueOf(l.getAccuracy()));
            mMotionCountText.setText(String.valueOf(mMotionCount));
            mMotionTimeText.setText(String.valueOf(mMotionCurrentTime-mMotionLastTime));
        }

/*
        function degreesToRadians(degrees) {
            return degrees * Math.PI / 180;
        }

        function distanceInKmBetweenEarthCoordinates(lat1, lon1, lat2, lon2) {
            var earthRadiusKm = 6371;

            var dLat = degreesToRadians(lat2-lat1);
            var dLon = degreesToRadians(lon2-lon1);

            lat1 = degreesToRadians(lat1);
            lat2 = degreesToRadians(lat2);

            var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                    Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
            var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
            return earthRadiusKm * c;
        }
*/
        if (s > 1) {
            Location l1, l2;
            l1 = (Location) mLocations.get(s-2);
            l2 = (Location) mLocations.get(s-1);
            double lat1, lat2, lon1, lon2;
            lat1 = l1.getLatitude()*Math.PI/180.0;
            lat2 = l2.getLatitude()*Math.PI/180.0;
            lon1 = l1.getLongitude()*Math.PI/180.0;
            lon2 = l2.getLongitude()*Math.PI/180.0;

            double dLat = lat2-lat1;
            double dLon = lon2-lon1;
            double a = Math.sin(dLat/2.0) * Math.sin(dLat/2.0) +
                    Math.sin(dLon/2.0) * Math.sin(dLon/2.0) * Math.cos(lat1) * Math.cos(lat2);
            double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0-a));
            double earthRadiusKm = 6371.0;
            double dKm = earthRadiusKm * c;
            double dMi = dKm * 0.621371;
            double dFt = dKm * 3280.84;

            mCumMiles += dMi;

            mFeetText.setText(String.valueOf(dFt));
            mCumMilesText.setText(String.valueOf(mCumMiles));

        }



    }

    /*
    ** GoogleApi related fields, overrides and methods
    */
    
    private GoogleApiClient mGoogleApiClient;

    protected void doInitGoogleLocationApi() {
        Log.d(TAG, "doInitGoogleLocationApi()");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected()");
        if (!mHaveLocationPermission) {
            doGetLocationPermission();
        }

        if (mHaveLocationPermission) {
            getLocation();
            if (mLastLocation != null) {
                mLatitudeText.setText(String.format(Locale.getDefault(), "%s: %f", mLatitudeLabel,
                        mLastLocation.getLatitude()));
                mLongitudeText.setText(String.format(Locale.getDefault(), "%s: %f", mLongitudeLabel,
                        mLastLocation.getLongitude()));
            } else {
                Log.d(TAG, "no last location");
            }
            createLocationRequest();
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.d(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /*
    ** Permission related fields, overrides and methods
    */

    private int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 51;
    private boolean mHaveLocationPermission = false;

    protected void doCheckLocationPermission() {
        Log.d(TAG, "doCheckLocationPermission()");
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "have location permission");
            mHaveLocationPermission = true;
        } else {
            Log.d(TAG, "do not have location permission");
            mHaveLocationPermission = false;
        }
    }
    
    protected void doGetLocationPermission() {
        Log.d(TAG, "doGetLocationPermission()");
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "FINE permission granted in onCreate()");
            mHaveLocationPermission = true;
        } else {
            Log.d(TAG, "FINE permission not granted during onCreate()");
            mHaveLocationPermission = false;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        if (requestCode == MY_PERMISSIONS_ACCESS_FINE_LOCATION) {
            Log.d(TAG, String.format("%s %d", permissions[0], grantResults[0]));
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult() - permission granted");
                mHaveLocationPermission = true;
                getLocation();
                createLocationRequest();
                startLocationUpdates();
            } else {
                Log.d(TAG, "onRequestPermissionsResult() - permission denied");
                mHaveLocationPermission = false;
            }
        }
    }

    /*
    ** Location related fields, overrides and methods
    */

    LocationRequest mLocationRequest;
    protected Location mLastLocation;
    protected Location mCurrentLocation;
    protected String mLastUpdateTime;
    protected int mCountUpdates;
    protected LinkedList mLocations = new LinkedList();

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged()");
        mLocations.addLast(location);
        updateUI();
    }

    protected void getLocation() {
        Log.d(TAG, "getLocation()");try {
            Location l = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLocations.addLast(l);
            updateUI();
        }
        catch (SecurityException e) {
            Log.d(TAG, "getLocation() security exception last location");
        }
    }

    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates()");
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
        catch (SecurityException e) {
            Log.d(TAG, "security exception location update");
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());
    }

    /*
    ** Significant motion sensor related fields, overrides and methods
    */

    private SensorManager mSigMotionSensorManager;
    private Sensor mSigMotionSensor;
    private TriggerEventListener mSigMotionTriggerEventListener;
    private int mMotionCount = 0;
    private long mMotionLastTime;
    private long mMotionCurrentTime;

}