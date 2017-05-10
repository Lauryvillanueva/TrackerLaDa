package com.uninorte.proyecto2;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    DatabaseReference mDatabase;
    private FirebaseAuth auth;

    protected final static String LOCATION_KEY = "location-key";

    protected static final String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mGoogleMap;
    private SupportMapFragment mapFrag;
    private LocationRequest mLocationRequest;
    private LocationManager mManager;
    private GoogleApiClient mGoogleApiClient;
    private Location mInitialLocation;
    private Location mFinalLocation;
    private Location mPreviousLocation,mCurrentLocation;
    private List<Location> mLocationsList;

    private Location mLastLocation;
    private Marker mCurrLocationMarker;
    private String recorridoid;

    private boolean enableGPS;


    private float mGoogleMapZoom = 17.0f;

    private Criteria mCriteria;
    private boolean mStayWithMap;

    private boolean recorrido;

    static final String STATE_STAY = "mStayWithMap";
    static final String STATE_MARK = "mAutoMark";
    static final String STATE_GPS_STATE = "mGpsStarted";

    private Button mButtonStart;

    private NotificationManager mNotificationManager;
    int mNotificationId = 001;


    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    int REQUEST_CODE = 1;

    private BroadcastReceiver mLocationReceiver;
    protected Boolean mRequestingLocationUpdates;
    private Intent mRequestLocationIntent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        mCriteria = new Criteria();
        mCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        mCriteria.setPowerRequirement(Criteria.POWER_LOW);


        mButtonStart = (Button) findViewById(R.id.ButtonStart);

        auth = FirebaseAuth.getInstance();
        recorrido =false;



        mManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }

        mLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mPreviousLocation = mCurrentLocation;
                mCurrentLocation = intent.getParcelableExtra(LocationUpdaterServices.COPA_MESSAGE);
                updateMap();
                mLocationsList.add(mCurrentLocation);

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "LocationList size: " + mLocationsList.size());
                }
            }
        };

        mRequestLocationIntent = new Intent(this, LocationUpdaterServices.class);
        startService(mRequestLocationIntent);

        updateValuesFromBundle(savedInstanceState);



}

    private void updateMap() {
        if (mPreviousLocation != null) {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude()))
                    .add(new LatLng(mPreviousLocation.getLatitude(),mCurrentLocation.getLongitude()))
                    .color(Color.RED)
                    .width(5);
            mGoogleMap.addPolyline(polylineOptions);
        }
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude()), 21f));
    }

    private void updateMap(List<LatLng> loc) {

        LatLng current;
        LatLng previous;
//      geo fix -3.7038395 40.416745
//      geo fix -3.7036161 40.4166984
//      geo fix -3.7039319 40.416653
//      geo fix -3.7042343 40.4165733
        for (int i = 0; i < loc.size() - 1; i++) {
            current = loc.get(i);
            previous = loc.get(i + 1);

            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(current)
                    .add(previous)
                    .color(Color.RED)
                    .width(5);

            mGoogleMap.addPolyline(polylineOptions);
//            mMap.addMarker(new MarkerOptions().position(current).title(current.toString()));
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 21f));
        }

    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Updating values from bundle");
        }
        if (savedInstanceState != null) {
            // Update the value of mLocationsList from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mLocationsList
                // is not null.
                mLocationsList = savedInstanceState.getParcelableArrayList(LOCATION_KEY);
                mCurrentLocation = mLocationsList.get(mLocationsList.size() - 1);
                if (mLocationsList.size() >= 2) {
                    mPreviousLocation = mLocationsList.get(mLocationsList.size() - 2);
                }
            }
        } else {
            mLocationsList = new ArrayList<>();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }


      @Override
    public void onLocationChanged(Location location) {

                  mLastLocation = location;
                  if (mCurrLocationMarker != null) {
                      mCurrLocationMarker.remove();
                  }

                  //Place current location marker
                  LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                  MarkerOptions markerOptions = new MarkerOptions();
                  markerOptions.position(latLng);
                  markerOptions.title("Posicion Actual");
                  markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                  mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

                  //move map camera
                  mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,11));
                  mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(mGoogleMapZoom));

      }

    //NOTIFICACION PARA CUANDO EL TRACKING ESTA CORRIENDO O PAUSADO
    public void onClickButtonStayButton(View view) {
       /* mStayWithMap = !mStayWithMap;
        boolean on = ((ToggleButton) view).isChecked();
        if (on==true) {
            String msj = "running";
            showNotification(msj);

        }else if (on==false){
            String msj = "paused";
            showNotification(msj);
        }else{
            //finish();
        }*/

    }

/*
    @Override
    public void onMapReady(GoogleMap googleMap) {

        googleMap.addMarker(new MarkerOptions().position(new LatLng(0,0)).title("Aqui Estoy"));


        mGoogleMap=googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);


        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);



            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }


    }*/

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        UiSettings uiSettings = mGoogleMap.getUiSettings();
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
    }



        @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);

                    }

                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //-----------------------------------------

    //-----------------------------------------

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppTheme_Dark_Dialog);
        builder.setTitle("GPS Desactivado");
        builder.setMessage("Su GPS se encuentra desactivado, desea activarlo?");
        builder.setCancelable(false);

        builder.setPositiveButton("Si", new DialogInterface.OnClickListener()
        {
            public void onClick(final DialogInterface dialog, final int id)
            {
                launchGPSOptions();

            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            public void onClick(final DialogInterface dialog, final int id)
            {
                dialog.cancel();

            }
        });

        builder.create().show();
    }

    private void launchGPSOptions()
    {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, REQUEST_CODE);
    }

    //-----------------------------------------


    private void showNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Proyecto2")
                        .setOngoing(true)
                        .setContentText("The tracking is running");
        Intent resultIntent = new Intent(this, MapsActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MapsActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(mNotificationId,mBuilder.build());
    }

    public void buildAlertMessageStartRecorrido(){

            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(MapsActivity.this, R.style.AppTheme_Dialog);
            final AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);

            builder.setTitle("Nuevo Recorrido");
            builder.setMessage("Nombre Recorrido");
            final EditText input = new EditText(MapsActivity.this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);

            builder.setView(input);
            input.setInputType(InputType.TYPE_CLASS_TEXT);

            builder.setCancelable(false);

            builder.setPositiveButton("Si", new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    String nombre = input.getText().toString();
                    Recorrido newRecorrido;
                    if (!TextUtils.isEmpty(nombre)) {
                        mDatabase = FirebaseDatabase.getInstance().getReference("recorridos");
                        recorridoid = mDatabase.push().getKey();
                        newRecorrido = new Recorrido(nombre, auth.getCurrentUser().getUid());
                        mDatabase.child(recorridoid).setValue(newRecorrido);
                        Toast.makeText(MapsActivity.this, "Recorrido Guardado ", Toast.LENGTH_SHORT).show();
                        recorrido = true;
                        mButtonStart.setText("Parada");
                        mInitialLocation = mLastLocation;

                    } else {
                        Toast.makeText(MapsActivity.this, "Recorrido No Guardado", Toast.LENGTH_SHORT).show();

                    }
                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, final int id) {
                    dialog.cancel();

                }
            });

            builder.create().show();

    }

    public void buildAlertMessageSaveTramo(){
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(MapsActivity.this,android.R.style.Theme_Dialog);
        final AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);

        builder.setTitle("Continuar Recorrido");
        builder.setMessage("Desea añadir otro Tramo");
        builder.setCancelable(false);

        builder.setPositiveButton("Si", new DialogInterface.OnClickListener()
        {
            public void onClick(final DialogInterface dialog, final int id)
            {
                mInitialLocation=mFinalLocation;
                recorrido=true;
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener()
        {
            public void onClick(final DialogInterface dialog, final int id)
            {
                mButtonStart.setText("Iniciar");
                recorrido=false;
                dialog.cancel();

            }
        });

        builder.create().show();



    }

    public void onClickButtonIniciar(View view) {

      // mManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
       // final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        //validar que gps este activado para empezar ------
        if ( !mManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            Toast.makeText(MapsActivity.this, "Error! GPS Desactivado", Toast.LENGTH_SHORT).show();
            buildAlertMessageNoGps();


        }else {

            if (!recorrido) {
                buildAlertMessageStartRecorrido();
            } else {
                mFinalLocation = mLastLocation;
                mDatabase = FirebaseDatabase.getInstance().getReference("tramos");
                String tramoid = mDatabase.push().getKey();
                Tramo newTramo = new Tramo(mInitialLocation, mFinalLocation);
                Toast.makeText(MapsActivity.this, "Tramo Guardado ", Toast.LENGTH_SHORT).show();
                mDatabase.child(recorridoid).child(tramoid).setValue(newTramo);
                buildAlertMessageSaveTramo();

            }
        }
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Cancelled scan");
                }
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Scanned");
                }
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                Matcher m = pat.matcher(result.getContents());
                int i = 0;
                while (m.find()) {
                    mCoord[i++] = Double.parseDouble(m.group(1));
                }
                // Add a marker and move the camera
                LatLng firstLocation = new LatLng(mCoord[0], mCoord[1]);
                mGoogleMap.addMarker(new MarkerOptions().position(firstLocation).title("Dest"));
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(firstLocation));
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 21.0f));

                GoogleDirection.withServerKey(getString(R.string.google_maps_server_key))
                        .from(mCurrentLocation)
                        .to(new LatLng(mCoord[0], mCoord[1]))
                        .transportMode(TransportMode.WALKING)
                        .execute(new DirectionCallback() {
                            @Override
                            public void onDirectionSuccess(Direction direction) {
                                if (direction.isOK()) {
                                    Toast.makeText(getApplicationContext(), "DIRECTION KOK", Toast.LENGTH_LONG).show();
                                    ArrayList<LatLng> directionPositionList = direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint();
                                    PolylineOptions polylineOptions = DirectionConverter.createPolyline(getApplicationContext(), directionPositionList, 5, Color.BLUE);
                                    mMap.addPolyline(polylineOptions);
                                } else {
                                    Toast.makeText(getApplicationContext(), "NOT OK" + direction.getStatus(), Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onDirectionFailure(Throwable t) {
                                Toast.makeText(getApplicationContext(), "Failure", Toast.LENGTH_LONG).show();
                            }
                        });

            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

*/
}
