package com.gogreen.greenmachine.main.match;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFloat;
import com.gogreen.greenmachine.R;
import com.gogreen.greenmachine.interBack.InterBack;
import com.gogreen.greenmachine.parseobjects.Hotspot;
import com.gogreen.greenmachine.parseobjects.MatchRoute;
import com.gogreen.greenmachine.parseobjects.PublicProfile;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DrivingHotspotSelectActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {

    private final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    private final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    private final static String LOCATION_KEY = "location-key";
    private final static String HOTSPOTS_KEY = "hotspots";

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Location mCurrentLocation;
    private double mLatitude;
    private double mLongitude;
    private boolean mRequestingLocationUpdates;
    private LocationRequest mLocationRequest;

    private String mLastUpdateTime;
    private GoogleMap mMap;

    private Toolbar toolbar;

    private MatchRoute matchRoute;
    private Set<Hotspot> serverHotspots;
    private int currentCapacity;
    private String driverCar;
    private Date matchByDate;
    private Date arriveByDate;
    private MatchRoute.Destination destination;
    private Set<Hotspot> selectedHotspots;
    private InterBack backend = new InterBack();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving_hotspot_select);

        // Process intent items
        this.currentCapacity = (int) getIntent().getExtras().get("capacity");
        this.matchByDate = convertToDateObject(getIntent().getExtras().get("matchDate").toString());
        this.arriveByDate = convertToDateObject(getIntent().getExtras().get("arriveDate").toString());
        this.destination = processDestination(getIntent().getExtras().get("destination").toString());
        this.driverCar = (String) getIntent().getExtras().get("driverCar");

        // Turn on location updates
        this.mRequestingLocationUpdates = true;

        // Grab server hotspots
        this.serverHotspots = backend.getAllHotspots();

        // Initialize selectedHotspots
        this.selectedHotspots = new HashSet<Hotspot>();

        // Set up the toolbar
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        ButtonFloat matchButton = (ButtonFloat) findViewById(R.id.buttonFloat);
        matchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Create a route and only execute it if it is succesfully created
                new FindMatchTask().execute();
            }
        });

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_driving_hotspot_select, menu);

        return super.onCreateOptionsMenu(menu);
    }

    private void startNextActivity() {
        Intent intent = new Intent(DrivingHotspotSelectActivity.this, DriverMatchedActivity.class);
        startActivity(intent);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (mLastLocation != null) {
            mLatitude = (mLastLocation.getLatitude());
            mLongitude = (mLastLocation.getLongitude());
        } else {
            Toast.makeText(getApplicationContext(),"Please turn on location.", Toast.LENGTH_SHORT).show();
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        //map can be loaded after the current location is known
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnectionSuspended(int code) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        updateLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
    }

    private void updateLocation() {
        if (mCurrentLocation != null) {
            mLatitude = mCurrentLocation.getLatitude();
            mLongitude = mCurrentLocation.getLongitude();
            backend.setUserLastKnownLocation(mLatitude, mLongitude);
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);

        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);

        // Save selected hotspots
        Iterator selectedHspotsIter = this.selectedHotspots.iterator();
        List<String> hSpotIds = new ArrayList<>();
        while (selectedHspotsIter.hasNext()) {
            Hotspot h = (Hotspot) selectedHspotsIter.next();
            hSpotIds.add(h.getObjectId());
        }
        savedInstanceState.putStringArrayList(HOTSPOTS_KEY, (ArrayList) hSpotIds);

        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }

            if (savedInstanceState.keySet().contains(HOTSPOTS_KEY)) {
                List<String> selectedHspotList = savedInstanceState.getStringArrayList(HOTSPOTS_KEY);
                Set<String> selectedHspotStrings = new HashSet<>(selectedHspotList);

                Iterator serverHotspotIter = this.serverHotspots.iterator();
                while (serverHotspotIter.hasNext()) {
                    Hotspot h = (Hotspot) serverHotspotIter.next();
                    if (selectedHspotStrings.contains(h.getObjectId())) {
                        // Set the opacity of the marker if it is actually a marker on the map
                        this.selectedHotspots.add(h);
                    }
                }
            }
            updateLocation();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    @Override
    public void onMapReady(GoogleMap map) {

        mMap = map;

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(mLatitude, mLongitude))      // Sets the center of the map
                .zoom(10)
                .build();                   // Creates a CameraPosition from the builder
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        if (this.serverHotspots != null) {
            Iterator iter = this.serverHotspots.iterator();
            while (iter.hasNext()) {
                Hotspot h = (Hotspot) iter.next();
                LatLng hotspotLoc = h.getHotspotLocation();
                MarkerOptions markerOptions = new MarkerOptions().position(hotspotLoc)
                        .icon(BitmapDescriptorFactory.defaultMarker(30))
                        .title(h.getName())
                        .alpha(0.75f);
                Marker marker = map.addMarker(markerOptions);
                map.setOnMarkerClickListener((GoogleMap.OnMarkerClickListener) this);

                if (this.selectedHotspots.contains(h)) {
                    setMarker(marker);
                }
            }
        } else {
            // Handle the server not getting hotspots
        }
        map.setMyLocationEnabled(true);
    }

    @Override
    public boolean onMarkerClick(Marker m) {
        if (m.getAlpha() == 0.75f) {
            setMarker(m);
        }
        else{
            resetMarker(m);
        }
        return true;
    }

    public void setMarker(Marker m){
        m.setIcon(BitmapDescriptorFactory.defaultMarker(150));
        m.setAlpha(1.0f);

        // Grab location & add to Hotspot set
        LatLng mPoint = m.getPosition();

        // Find the original hotspot item and add it to the set
        Iterator iter = this.serverHotspots.iterator();
        while (iter.hasNext()) {
            Hotspot hSpot = (Hotspot) iter.next();
            backend.fetchIfNeeded(hSpot);
            if (hSpot.isHotspotAtLocation(mPoint)) {
                this.selectedHotspots.add(hSpot);
                break;
            }
        }
    }

    public void resetMarker(Marker m){
        m.setAlpha(0.75f);
        m.setIcon(BitmapDescriptorFactory.defaultMarker(30));

        LatLng mPoint = m.getPosition();

        // Find the original hotspot item and add it to the set
        Iterator iter = this.serverHotspots.iterator();
        while (iter.hasNext()) {
            Hotspot hSpot = (Hotspot) iter.next();
            backend.fetchIfNeeded(hSpot);
            if (hSpot.isHotspotAtLocation(mPoint)) {
                this.selectedHotspots.remove(hSpot);
                break;
            }
        }
    }

    private boolean checkForRiders() {
        // MatchRoute should be created so now we peridically check if a rider gets added to our request

        try {
            this.matchRoute.fetch();
        } catch (ParseException e) {
            return false;
        }
        ArrayList<PublicProfile> riders = this.matchRoute.getRiders();
        boolean foundRider = !riders.isEmpty();
        if (foundRider) {
            return true;
        } else {
            return false;
        }
    }

    private boolean createMatchRoute() {
        // Create a match route
        this.matchRoute = new MatchRoute();
        ArrayList<Hotspot> selectedHotspotsList = new ArrayList<Hotspot>(selectedHotspots);
        matchRoute.initializeMatchRoute(ParseUser.getCurrentUser(), selectedHotspotsList, destination,
                MatchRoute.TripStatus.NOT_STARTED, currentCapacity, matchByDate,
                arriveByDate, driverCar, new ArrayList<PublicProfile>());
        try {
            matchRoute.save();
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private void processResult() {
        ArrayList<PublicProfile> riders = this.matchRoute.getRiders();
        if (riders.isEmpty()) {
            Toast.makeText(DrivingHotspotSelectActivity.this, getString(R.string.progress_no_rider_found), Toast.LENGTH_SHORT).show();
        } else {
            startNextActivity();
        }
    }

    private Date convertToDateObject(String s) {
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd h:m a");
        Calendar cal = Calendar.getInstance();
        String input = cal.get(Calendar.YEAR)+"-"+(cal.get(Calendar.MONTH)+1)+"-"+cal.get(Calendar.DATE)+" " +s;

        Date t = new Date();

        try {
            t = ft.parse(input);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return t;
    }

    private MatchRoute.Destination processDestination(String s) {
        if (s.equals("HQ")) {
            return MatchRoute.Destination.HQ;
        } else {
            return MatchRoute.Destination.HOME;
        }
    }

    private class FindMatchTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(DrivingHotspotSelectActivity.this);
        Boolean routeCreated = false;
        Boolean riderFound = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pdLoading.setMessage(getString(R.string.progress_matching_driver));
            pdLoading.show();
            pdLoading.setCancelable(false);
            pdLoading.setCanceledOnTouchOutside(false);
        }
        @Override
        protected Void doInBackground(Void... params) {
            for (int i = 0; i < 100; i++) {
                if (!routeCreated) {
                    routeCreated = createMatchRoute();
                } else if (!riderFound) {
                    riderFound = checkForRiders();
                } else if (riderFound) {
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            matchRoute.setStatus(MatchRoute.TripStatus.CANCELED);
            matchRoute.saveInBackground();
            pdLoading.dismiss();
            processResult();
        }
    }
}
