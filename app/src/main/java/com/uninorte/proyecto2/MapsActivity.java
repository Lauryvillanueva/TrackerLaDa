package com.uninorte.proyecto2;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
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


import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.util.DirectionConverter;
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
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback{
    DatabaseReference mDatabase;
    private FirebaseAuth auth;

    private static final Pattern pat = Pattern.compile("[A-Z]+_(-?\\d+\\.\\d+)");

    protected final static String LOCATION_KEY = "location-key";
    private double[] mCoord = new double[2];

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

    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
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

        mRequestingLocationUpdates = true;

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
                LatLng cLocation =intent.getParcelableExtra(LocationUpdaterServices.COPA_MESSAGE);
                mCurrentLocation = new Location("");
                mCurrentLocation.setLatitude(cLocation.latitude);
                mCurrentLocation.setLongitude(cLocation.longitude);
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

    private void updateMap(List<Location> loc) {

        LatLng current;
        LatLng previous;
        for (int i = 0; i < loc.size() - 1; i++) {
            current = new LatLng(loc.get(i).getLatitude(),loc.get(i).getLongitude());
            previous = new LatLng(loc.get(i + 1).getLatitude(),loc.get(i + 1).getLongitude());

            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(current)
                    .add(previous)
                    .color(Color.RED)
                    .width(5);

            mGoogleMap.addPolyline(polylineOptions);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 21f));
        }

    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Updating values from bundle");
        }
        if (savedInstanceState != null) {

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {

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

        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, new IntentFilter(LocationUpdaterServices.COPA_RESULT));
        if (mLocationsList != null && mGoogleMap != null) {
            updateMap(mLocationsList);
        }
    }



    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(mRequestLocationIntent);

        super.onDestroy();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);

        savedInstanceState.putParcelableArrayList(LOCATION_KEY, (ArrayList<? extends Parcelable>) mLocationsList);
//        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

/*
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

      }*/

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
        uiSettings.setMyLocationButtonEnabled(true);

        if (mLocationsList != null) {
            updateMap(mLocationsList);
        }
    }


/*
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
                .addConnectionCallbacks(MapsActivity.this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }*/

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {


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

    @Override
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
                Log.d(TAG, "onActivityResult: "+firstLocation.latitude+","+firstLocation.longitude);
                mGoogleMap.addMarker(new MarkerOptions().position(firstLocation).title("Dest"));
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(firstLocation));
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 21.0f));

                GoogleDirection.withServerKey(getString(R.string.google_maps_key))
                        .from(new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude()))
                        .to(new LatLng(mCoord[0], mCoord[1]))
                        .transportMode(TransportMode.WALKING)
                        .execute(new DirectionCallback() {
                            @Override
                            public void onDirectionSuccess(Direction direction, String rawBody) {
                                if (direction.isOK()) {
                                    Toast.makeText(getApplicationContext(), "DIRECTION KOK", Toast.LENGTH_LONG).show();
                                    ArrayList<LatLng> directionPositionList = direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint();
                                    PolylineOptions polylineOptions = DirectionConverter.createPolyline(getApplicationContext(), directionPositionList, 5, Color.BLUE);
                                    mGoogleMap.addPolyline(polylineOptions);
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


}
