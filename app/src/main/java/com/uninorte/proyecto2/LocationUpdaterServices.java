package com.uninorte.proyecto2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.compat.*;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.Manifest;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Erwin on 08/05/2017.
 */

public class LocationUpdaterServices extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    public static final String COPA_RESULT = "com.uninorte.proyecto2.REQUEST_PROCESSED";

    public static final String COPA_MESSAGE = "com.uninorte.proyecto2.COPA_MSG";

    protected static final String TAG = LocationUpdaterServices.class.getSimpleName();

    protected GoogleApiClient mGoogleApiClient;

    protected LocationRequest mLocationRequest;

    protected Location mCurrentLocation;

    protected String mLastUpdateTime;

    private LocalBroadcastManager mBroadcaster;

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        mBroadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onDestroy() {

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Stopping Service");
        }

        stopLocationUpdates();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);
            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Connected to GoogleApiClient");
        }
//Manifest.permission.ACCESS_FINE_LOCATION

        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            if (mCurrentLocation != null) {
                sendResult(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, mCurrentLocation.getLatitude() + ", " + mCurrentLocation.getLongitude());
                }
            }
        }


        startLocationUpdates();


    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection  to Google Api Client suspended");
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        sendResult(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));

        if (BuildConfig.DEBUG) {
            Log.d(TAG, mCurrentLocation.getLatitude() + ", " + mCurrentLocation.getLongitude());
        }

    }

    protected synchronized void buildGoogleApiClient(){
        if(BuildConfig.DEBUG){
            Log.d(TAG, "Building GoogleApiClient");
        }
        mGoogleApiClient= new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

         createLocationRequest();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "OnStartCommand");
        }

        if (mGoogleApiClient.isConnected() /*&& mRequestingLocationUpdates*/) { // TODO
            startLocationUpdates();
        }

        // TODO: Call it here, or stopService from Activity stopSelf();

        return START_STICKY;
    }


    private void sendResult(LatLng message) {
        Intent intent = new Intent(COPA_RESULT);
        if (message != null)
            intent.putExtra(COPA_MESSAGE, message);
        mBroadcaster.sendBroadcast(intent);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
    }



    protected void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


}
